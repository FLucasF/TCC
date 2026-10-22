package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pousada.persistence.Database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Teste ponta a ponta; usa o banco de DB_URL num schema próprio e descartável (ignorado sem DB_URL). */
class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static String adminUrl, user, password;
    private static Database db;
    private static Javalin app;

    @BeforeAll
    static void start() throws Exception {
        adminUrl = System.getenv("DB_URL");
        assumeTrue(adminUrl != null, "DB_URL não definido");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        schema = "teste_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(adminUrl, user, password); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        String sep = adminUrl.contains("?") ? "&" : "?";
        db = new Database(adminUrl + sep + "currentSchema=" + schema, user, password);
        app = App.start(0, db);
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(adminUrl, user, password); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private record Res(int status, String body) {
        JsonNode json() throws Exception {
            return JSON.readTree(body);
        }
    }

    private static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body());
    }

    private static String reservation(int room, String identity, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com",%s,
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, identity, in, out,
                guests, plan);
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void fullFlow() throws Exception {
        Res created = call("POST", "/reservations", reservation(201, CPF, "2031-03-10", "2031-03-12", 2, "STANDARD"));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(779.80, r.get("total").asDouble());
        assertTrue(r.get("companyCnpj").isNull());
        assertTrue(r.get("refund").isNull());

        assertEquals(409, call("POST", "/reservations",
                reservation(201, CPF, "2031-03-11", "2031-03-13", 2, "STANDARD")).status());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).status());

        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("CONFIRMED", confirmed.json().get("status").asText());
        assertEquals(779.80, confirmed.json().get("amountPaid").asDouble());
        assertTrue(call("GET", "/messages", null).body().contains("Comprovante de reserva nº " + id));

        Res csv = call("GET", "/reports/occupancy?date=2031-03-11", null);
        assertTrue(csv.body().contains("201,DELUXE," + id + ",2031-03-10,2031-03-12"));
        assertTrue(csv.body().endsWith("total,1/5\n"));
        assertTrue(call("GET", "/reports/occupancy?date=2031-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertTrue(out.json().get("text").asText().startsWith("Comprovante de reserva nº " + id));
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());

        Res revenue = call("GET", "/reports/revenue?month=2031-03", null);
        assertTrue(revenue.body().contains(id + ",201,2031-03-10,2031-03-12,CHECKED_OUT,779.80"));
    }

    @Test
    void cancellationAndValidation() throws Exception {
        String passport = "\"guestPassport\":\"X1\",\"guestCountry\":\"UK\"";
        Res created = call("POST", "/reservations", reservation(301, passport, "2032-07-10", "2032-07-11", 4,
                "NON_REFUNDABLE"));
        long id = created.json().get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        Res cancelled = call("POST", "/reservations/" + id + "/cancellation", null);
        assertEquals(0.0, cancelled.json().get("refund").asDouble());
        assertTrue(call("GET", "/reports/revenue?month=2032-07", null).body().contains("CANCELLED"));
        // quarto liberado após cancelamento
        assertEquals(201, call("POST", "/reservations",
                reservation(301, passport, "2032-07-10", "2032-07-11", 1, "STANDARD")).status());

        assertEquals(422, call("POST", "/reservations",
                reservation(101, CPF, "2033-01-01", "2033-01-02", 3, "STANDARD")).status());
        assertEquals(404, call("POST", "/reservations",
                reservation(999, CPF, "2033-01-01", "2033-01-02", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, CPF + ",\"companyCnpj\":\"1\",\"companyName\":\"X\"", "2033-01-01", "2033-01-02", 1,
                        "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, CPF, "2033-01-02", "2033-01-02", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", "não é json").status());
        assertEquals(404, call("GET", "/reservations/99999", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2033-13", null).status());
        assertEquals(5, call("GET", "/rooms", null).json().size());
    }
}
