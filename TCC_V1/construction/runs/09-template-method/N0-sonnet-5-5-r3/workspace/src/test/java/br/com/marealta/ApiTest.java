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
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** End-to-end test against the PostgreSQL given in DB_URL; uses a throw-away schema. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ApiTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static String baseDb;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        baseDb = System.getenv("DB_URL");
        schema = "t_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = connect(baseDb); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        String url = baseDb + (baseDb.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        app = App.start(new Config(0, url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"), Config.DEFAULT_TOURISM_TAX));
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        try (Connection c = connect(baseDb); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private static Connection connect(String url) throws Exception {
        return DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
    }

    private record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception { return JSON.readTree(body); }
    }

    private static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        Res rooms = call("GET", "/rooms", null);
        assertEquals(5, rooms.json().size());
        assertEquals(389.90, rooms.json().get(2).get("nightlyRate").asDouble());

        Res created = call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.status());
        long id = created.json().get("id").asLong();
        assertEquals("PENDING", created.json().get("status").asText());
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertTrue(created.json().get("refund").isNull());

        assertEquals(409, call("POST", "/reservations", booking(201, "2027-03-11", "2027-03-13", 2, "STANDARD")).status());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 3, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-13", "2027-03-13", 1, "STANDARD")).status());
        assertEquals(404, call("POST", "/reservations", booking(999, "2027-03-11", "2027-03-13", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", "nope").status());

        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).status());
        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("CONFIRMED", confirmed.json().get("status").asText());
        assertTrue(confirmed.body().contains("\"amountPaid\":779.80"));
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode msgs = call("GET", "/messages", null).json();
        assertEquals(1, msgs.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());
        assertTrue(msgs.get(0).get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva"));

        Res occ = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertEquals("text/csv; charset=utf-8", occ.contentType.toLowerCase().replace("charset=utf-8", "charset=utf-8"));
        assertEquals("Pousada Maré Alta,occupancy,2027-03-10\nroom,type,reservation,check_in,check_out\n"
                + "201,DELUXE," + id + ",2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));

        Res tax = call("GET", "/reports/tourism-tax?month=2027-03", null);
        assertTrue(tax.body().contains(",2,2,18.00\n"), tax.body());
        assertTrue(tax.body().endsWith("total,18.00\n"));
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).status());
        assertEquals(400, call("GET", "/reports/revenue", null).status());

        assertEquals("CHECKED_IN", call("POST", "/reservations/" + id + "/check-in", null).json().get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals("DELUXE", out.json().get("roomType").asText());
        assertEquals(out.json(), call("GET", "/reservations/" + id + "/document", null).json());
    }

    @Test
    void cancellationsAndPlanReport() throws Exception {
        long nr = call("POST", "/reservations", booking(301, "2027-08-01", "2027-08-03", 2, "NON_REFUNDABLE")).json().get("id").asLong();
        call("POST", "/reservations/" + nr + "/confirmation", null);
        Res cancelled = call("POST", "/reservations/" + nr + "/cancellation", null);
        assertEquals("CANCELLED", cancelled.json().get("status").asText());
        assertEquals(0.0, cancelled.json().get("refund").asDouble());
        // room is free again, and the pending cancellation leaves nothing
        long again = call("POST", "/reservations", booking(301, "2027-08-01", "2027-08-03", 2, "STANDARD")).json().get("id").asLong();
        Res c2 = call("POST", "/reservations/" + again + "/cancellation", null);
        assertEquals(0.0, c2.json().get("refund").asDouble());
        long std = call("POST", "/reservations", booking(301, "2027-08-05", "2027-08-06", 2, "STANDARD")).json().get("id").asLong();
        call("POST", "/reservations/" + std + "/confirmation", null);
        Res c3 = call("POST", "/reservations/" + std + "/cancellation", null);
        assertEquals(579.90, c3.json().get("refund").asDouble());

        Res plans = call("GET", "/reports/rate-plans?month=2027-08", null);
        assertEquals("Pousada Maré Alta,rate-plans,2027-08\nrate_plan,reservations,amount\nSTANDARD,0,0.00\n"
                + "NON_REFUNDABLE,1,985.83\ntotal,1,985.83\n", plans.body());
        Res rev = call("GET", "/reports/revenue?month=2027-08", null);
        assertTrue(rev.body().contains(",CANCELLED,985.83\n"), rev.body());
        assertEquals(404, call("GET", "/reservations/abc", null).status());
    }
}
