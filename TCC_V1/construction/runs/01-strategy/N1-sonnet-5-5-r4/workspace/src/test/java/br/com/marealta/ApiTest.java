package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Ponta a ponta contra o PostgreSQL de DB_URL, num schema descartável. Pulado se DB_URL não existir. */
class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String url;
    private static String user;
    private static String password;
    private static String schema;
    private static Database db;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        String dbUrl = System.getenv("DB_URL");
        Assumptions.assumeTrue(dbUrl != null && !dbUrl.isBlank(), "DB_URL ausente");
        url = dbUrl;
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        schema = "teste_" + UUID.randomUUID().toString().replace("-", "");
        admin("CREATE SCHEMA " + schema);
        db = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        app = new Routes(new ReservationService(db), new ReportService(db)).build().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
            db.close();
            admin("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private static void admin(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception {
            return JSON.readTree(body);
        }
    }

    private static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    private static String booking(int room, String in, String out, int guests, String plan, String company) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"%s}"""
                .formatted(room, in, out, guests, plan, company == null ? "" : ",\"companyCode\":\"" + company + "\"");
    }

    @Test
    void fullFlowWithCorporatePlan() throws Exception {
        Res created = call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", "TECNOSUL"));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertEquals("686.22", r.get("total").asText());
        assertEquals("93.58", r.get("discount").asText());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("refund").isNull());

        // sem pagamento, não há segunda via
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());

        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("686.22", confirmed.json().get("amountPaid").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode messages = call("GET", "/messages", null).json();
        assertEquals(1, messages.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
        assertEquals("EMAIL", messages.get(0).get("channel").asText());

        Res occupancy = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertTrue(occupancy.contentType().toLowerCase().contains("text/csv"));
        assertTrue(occupancy.body().endsWith("total,1/5\n"));
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).status());

        Res revenue = call("GET", "/reports/revenue?month=2027-03", null);
        assertTrue(revenue.body().contains("CHECKED_OUT,686.22"));
        assertTrue(revenue.body().endsWith("total,686.22\n"));
    }

    @Test
    void cancellationRefunds() throws Exception {
        long id = call("POST", "/reservations", booking(301, "2027-03-12", "2027-03-14", 4, "WEEKEND_PACKAGE", null))
                .json().get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        Res cancelled = call("POST", "/reservations/" + id + "/cancellation", null);
        assertTrue(cancelled.body().contains("\"refund\":600.00"), cancelled.body());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).status());
        // cancelada libera o quarto
        assertEquals(201, call("POST", "/reservations", booking(301, "2027-03-12", "2027-03-14", 1, "STANDARD", null)).status());
    }

    @Test
    void errors() throws Exception {
        assertEquals("ROOM_NOT_FOUND", call("POST", "/reservations",
                booking(999, "2027-05-10", "2027-05-12", 1, "STANDARD", null)).json().get("error").asText());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-05-10", "2027-05-12", 3, "STANDARD", null)).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-05-12", "2027-05-12", 1, "STANDARD", null)).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-05-10", "2027-05-12", 1, "CORPORATE", "NADA")).status());
        assertEquals(400, call("POST", "/reservations", "não é json").status());
        assertEquals(201, call("POST", "/reservations", booking(102, "2027-05-10", "2027-05-12", 1, "STANDARD", null)).status());
        assertEquals("ROOM_UNAVAILABLE", call("POST", "/reservations",
                booking(102, "2027-05-11", "2027-05-13", 1, "STANDARD", null)).json().get("error").asText());
        assertEquals(404, call("GET", "/reservations/99999", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).status());
        assertEquals(5, call("GET", "/rooms", null).json().size());
    }
}
