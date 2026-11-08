package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Roda contra o PostgreSQL de DB_URL, num schema próprio que é apagado no fim. */
class ApiIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static String url;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        String dbUrl = System.getenv("DB_URL");
        Assumptions.assumeTrue(dbUrl != null, "DB_URL não definido");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(dbUrl, user, password); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        url = dbUrl + (dbUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        app = App.build(new Db(url, user, password)).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private static JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = send("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return JSON.readTree(r.body());
    }

    @Test
    void fullFlowAndReports() throws Exception {
        assertEquals(5, JSON.readTree(send("GET", "/rooms", null).body()).size());

        JsonNode r = create(201, "2031-07-10", "2031-07-12", 2, "NON_REFUNDABLE");
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(0, r.get("amountPaid").decimalValue().signum());
        assertTrue(r.get("refund").isNull());
        assertTrue(send("GET", "/reservations/" + id, null).body().contains("\"amountPaid\":0.00"));

        assertEquals(409, send("POST", "/reservations", booking(201, "2031-07-11", "2031-07-13", 1, "STANDARD")).statusCode());
        assertEquals(422, send("POST", "/reservations", booking(101, "2031-07-11", "2031-07-13", 3, "STANDARD")).statusCode());
        assertEquals(400, send("POST", "/reservations", "nao json").statusCode());
        assertEquals(404, send("POST", "/reservations", booking(999, "2031-07-11", "2031-07-13", 1, "STANDARD")).statusCode());
        assertEquals(409, send("POST", "/reservations/" + id + "/check-in", null).statusCode());
        assertEquals(409, send("GET", "/reservations/" + id + "/document", null).statusCode());

        JsonNode confirmed = JSON.readTree(send("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(confirmed.get("total").decimalValue().toPlainString(), confirmed.get("amountPaid").decimalValue().toPlainString());

        JsonNode messages = JSON.readTree(send("GET", "/messages", null).body());
        JsonNode sent = null;
        for (JsonNode m : messages) {
            if (m.get("body").asText().contains("Comprovante de reserva nº " + id + "\n")) {
                sent = m;
            }
        }
        assertTrue(sent != null);
        assertEquals("Reserva confirmada - Pousada Maré Alta", sent.get("subject").asText());
        assertEquals("EMAIL", sent.get("channel").asText());
        assertEquals("carla@x.com", sent.get("to").asText());

        String csv = send("GET", "/reports/occupancy?date=2031-07-10", null).body();
        assertTrue(csv.startsWith("Pousada Maré Alta,occupancy,2031-07-10\nroom,type,reservation,check_in,check_out\n"), csv);
        assertTrue(csv.endsWith("total,1/5\n"), csv);
        assertTrue(send("GET", "/reports/occupancy?date=2031-07-12", null).body().endsWith("total,0/5\n"));

        String tax = send("GET", "/reports/tourism-tax?month=2031-07", null).body();
        assertTrue(tax.contains(",2031-07-10,2031-07-12,2,2,18.00\n"), tax);
        assertTrue(tax.endsWith("total,18.00\n"), tax);

        JsonNode cancelled = JSON.readTree(send("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals(0, cancelled.get("refund").decimalValue().signum());

        String revenue = send("GET", "/reports/revenue?month=2031-07", null).body();
        assertTrue(revenue.contains(",CANCELLED,"), revenue);
        String plans = send("GET", "/reports/rate-plans?month=2031-07", null).body();
        assertTrue(plans.contains("\nSTANDARD,0,0.00\nNON_REFUNDABLE,1,"), plans);
        assertTrue(plans.endsWith("total,1,795.40\n"), plans);
        assertEquals(400, send("GET", "/reports/revenue?month=2031-7", null).statusCode());
        assertEquals("text/csv; charset=utf-8", send("GET", "/reports/revenue?month=2031-07", null)
                .headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void checkOutReturnsReceipt() throws Exception {
        long id = create(101, "2032-03-01", "2032-03-02", 1, "STANDARD").get("id").asLong();
        send("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, send("POST", "/reservations/" + id + "/check-in", null).statusCode());
        JsonNode receipt = JSON.readTree(send("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("STANDARD", receipt.get("roomType").asText());
        assertEquals(receipt, JSON.readTree(send("GET", "/reservations/" + id + "/document", null).body()));
        assertEquals(409, send("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        assertEquals(404, send("GET", "/reservations/99999", null).statusCode());
    }
}
