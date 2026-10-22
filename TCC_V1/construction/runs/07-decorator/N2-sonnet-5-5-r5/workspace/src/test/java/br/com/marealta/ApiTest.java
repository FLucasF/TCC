package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.math.BigDecimal;
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

/** Runs against the PostgreSQL in DB_URL, inside a schema of its own. Skipped when there is no database. */
class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static Javalin app;
    private static String schema;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null, "DB_URL not set");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        Database database = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.initialize();
        app = Main.createApp(database, true).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String reservation(int room, String in, String out, int guests, String plan, String email) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    private static JsonNode carlas(JsonNode messages) {
        for (JsonNode m : messages) {
            if (m.get("body").asText().contains("carla@x.com")) {
                return m;
            }
        }
        throw new AssertionError("message not found");
    }

    @Test
    void fullFlow() throws Exception {
        assertEquals(5, JSON.readTree(call("GET", "/rooms", null).body()).size());

        HttpResponse<String> created = call("POST", "/reservations",
                reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD", "carla@x.com"));
        assertEquals(201, created.statusCode());
        JsonNode r = JSON.readTree(created.body());
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(created.body().contains("\"total\":779.80"));
        assertTrue(r.get("refund").isNull());

        // overlapping, over capacity, bad requests
        assertEquals(409, call("POST", "/reservations",
                reservation(201, "2027-03-11", "2027-03-13", 2, "STANDARD", "a@b.com")).statusCode());
        assertEquals(422, call("POST", "/reservations",
                reservation(101, "2027-03-11", "2027-03-13", 3, "STANDARD", "a@b.com")).statusCode());
        assertEquals(404, call("POST", "/reservations",
                reservation(999, "2027-03-11", "2027-03-13", 1, "STANDARD", "a@b.com")).statusCode());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, "2027-03-13", "2027-03-13", 1, "STANDARD", "a@b.com")).statusCode());
        assertEquals(400, call("POST", "/reservations", "not json").statusCode());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());

        JsonNode confirmed = JSON.readTree(call("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(0, new BigDecimal("779.80").compareTo(confirmed.get("amountPaid").decimalValue()));

        JsonNode messages = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals("testes@pousadamarealta.com.br", carlas(messages).get("to").asText());
        assertTrue(carlas(messages).get("body").asText().startsWith("Destinatário original: carla@x.com\nOlá, Carla Dias!"));

        HttpResponse<String> occupancy = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertTrue(occupancy.body().contains("201,DELUXE," + id + ",2027-03-10,2027-03-12\n"));
        assertTrue(occupancy.body().endsWith("total,1/5\n"));
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));
        assertEquals(400, call("GET", "/reports/occupancy?date=x", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        JsonNode receipt = JSON.readTree(call("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals(0, new BigDecimal("779.80").compareTo(receipt.get("total").decimalValue()));
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        assertEquals(404, call("GET", "/reservations/99999", null).statusCode());
    }

    @Test
    void nonRefundableCancellationKeepsRevenue() throws Exception {
        long id = JSON.readTree(call("POST", "/reservations",
                reservation(301, "2028-05-01", "2028-05-02", 4, "NON_REFUNDABLE", "ana@gmail")).body()).get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(0, cancelled.get("refund").decimalValue().signum());

        String revenue = call("GET", "/reports/revenue?month=2028-05", null).body();
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + id + ",301,2028-05-01,2028-05-02,CANCELLED,492.92\ntotal,492.92\n", revenue);
        // the room is free again
        assertEquals(201, call("POST", "/reservations",
                reservation(301, "2028-05-01", "2028-05-02", 1, "STANDARD", "a@b.com")).statusCode());
    }
}
