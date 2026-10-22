package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import pousada.persistence.Database;

/** Roda contra o PostgreSQL de DB_URL, num schema descartável. Pulado se o banco não estiver configurado. */
class ApiIntegrationTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String schema;
    static String url, user, password;
    static Javalin app;
    static String base;

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "DB_URL ausente");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        schema = "t_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = DriverManager.getConnection(url, user, password); var st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        String scoped = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        app = Main.start(new Database(scoped, user, password), false, 0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
        }
        if (schema != null) {
            try (var c = DriverManager.getConnection(url, user, password); var st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    static HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String reservation(int room, String in, String out, int guests, String plan, String email) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        var rooms = JSON.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble());

        var created = call("POST", "/reservations", reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD", "carla@x.com"));
        assertEquals(201, created.statusCode());
        JsonNode r = JSON.readTree(created.body());
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertTrue(created.body().contains("\"amountPaid\":0.00"), created.body());
        assertTrue(r.get("refund").isNull());

        assertEquals(409, call("POST", "/reservations", reservation(201, "2027-03-11", "2027-03-13", 2, "STANDARD", "a@b.com")).statusCode());
        assertEquals(422, call("POST", "/reservations", reservation(101, "2027-03-11", "2027-03-13", 3, "STANDARD", "a@b.com")).statusCode());
        assertEquals(400, call("POST", "/reservations", "{nope").statusCode());
        assertEquals(404, call("POST", "/reservations", reservation(999, "2027-03-11", "2027-03-13", 1, "STANDARD", "a@b.com")).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());

        int before = JSON.readTree(call("GET", "/messages", null).body()).size();
        var confirmed = JSON.readTree(call("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(779.80, confirmed.get("amountPaid").asDouble());

        var messages = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(before + 2, messages.size());
        assertEquals("carla@x.com", messages.get(before).get("to").asText());
        assertEquals("SENT", messages.get(before).get("status").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(before + 1).get("subject").asText());
        assertTrue(messages.get(before).get("body").asText().contains(
                "Comprovante de reserva nº " + id + "\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n"
                        + "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada"));

        var occupancy = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertTrue(occupancy.body().endsWith("total,1/5\n"), occupancy.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).statusCode());

        assertEquals("CHECKED_IN", JSON.readTree(call("POST", "/reservations/" + id + "/check-in", null).body()).get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        var receipt = JSON.readTree(call("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());
    }

    @Test
    void nonRefundableCancellationKeepsMoney() throws Exception {
        var created = call("POST", "/reservations", reservation(301, "2027-05-10", "2027-05-11", 1, "NON_REFUNDABLE", "ana@gmail"));
        long id = JSON.readTree(created.body()).get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        var cancelled = JSON.readTree(call("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(0.0, cancelled.get("refund").asDouble());
        var csv = call("GET", "/reports/revenue?month=2027-05", null).body();
        assertTrue(csv.startsWith("reservation,room,check_in,check_out,status,amount\n"));
        assertTrue(csv.contains(id + ",301,2027-05-10,2027-05-11,CANCELLED,492.92\n"), csv);
        assertTrue(csv.endsWith("total,492.92\n"), csv);
        var messages = JSON.readTree(call("GET", "/messages", null).body());
        assertTrue(messages.toString().contains("\"status\":\"BLOCKED\""));
    }
}
