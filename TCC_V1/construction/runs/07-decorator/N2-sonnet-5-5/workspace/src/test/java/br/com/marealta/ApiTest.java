package br.com.marealta;

import br.com.marealta.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/** Testa a API de ponta a ponta num schema descartável do PostgreSQL; é pulado se não houver banco. */
class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static String url, user, password;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        Assumptions.assumeTrue(url != null, "DB_URL não definida");
        schema = "teste_" + System.nanoTime();
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            Assumptions.abort("Banco indisponível: " + e.getMessage());
        }
        String scoped = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        app = App.build(new Database(scoped, user, password), false).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
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
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"11122233344",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        assertEquals(5, JSON.readTree(call("GET", "/rooms", null).body()).size());

        HttpResponse<String> created = call("POST", "/reservations", reservation(201, "2030-03-10", "2030-03-12", 2, "STANDARD", "carla@example.com"));
        assertEquals(201, created.statusCode());
        JsonNode r = JSON.readTree(created.body());
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(created.body().contains("\"amountPaid\":0.00"));
        assertTrue(r.get("refund").isNull());
        assertTrue(created.body().contains("\"total\":779.80"));

        assertEquals(409, call("POST", "/reservations", reservation(201, "2030-03-11", "2030-03-13", 2, "STANDARD", "c@example.com")).statusCode());
        assertEquals(422, call("POST", "/reservations", reservation(101, "2030-03-11", "2030-03-13", 3, "STANDARD", "c@example.com")).statusCode());
        assertEquals(404, call("POST", "/reservations", reservation(999, "2030-03-11", "2030-03-13", 1, "STANDARD", "c@example.com")).statusCode());
        assertEquals(400, call("POST", "/reservations", "{nao json").statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(201, "2030-03-12", "2030-03-12", 1, "STANDARD", "c@example.com")).statusCode());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());

        JsonNode confirmed = JSON.readTree(call("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(0, new java.math.BigDecimal("779.80").compareTo(confirmed.get("amountPaid").decimalValue()));

        JsonNode messages = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(2, messages.size());
        assertEquals("carla@example.com", messages.get(0).get("to").asText());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).get("subject").asText());

        HttpResponse<String> occupancy = call("GET", "/reports/occupancy?date=2030-03-11", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertTrue(occupancy.body().endsWith("total,1/5\n"));
        assertEquals(400, call("GET", "/reports/revenue?month=2030-3", null).statusCode());
        assertTrue(call("GET", "/reports/revenue?month=2030-03", null).body().contains("779.80"));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        JsonNode receipt = JSON.readTree(call("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(404, call("GET", "/reservations/99999", null).statusCode());
    }

    @Test
    void invalidEmailBlockedAndRefundOnCancel() throws Exception {
        JsonNode r = JSON.readTree(call("POST", "/reservations", reservation(301, "2030-05-10", "2030-05-11", 4, "NON_REFUNDABLE", "ana@gmail")).body());
        long id = r.get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals(0, cancelled.get("refund").decimalValue().signum());
        JsonNode messages = JSON.readTree(call("GET", "/messages", null).body());
        boolean blocked = false;
        for (JsonNode m : messages) {
            if (m.get("to").asText().equals("ana@gmail")) {
                assertEquals("BLOCKED", m.get("status").asText());
                blocked = true;
            }
        }
        assertTrue(blocked);
    }
}
