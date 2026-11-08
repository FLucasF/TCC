package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.marealta.pousada.persistence.Database;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Roda contra o PostgreSQL de DB_URL, num schema próprio; é ignorado se o banco não estiver acessível. */
class ApiIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static Database db;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null, "DB_URL não definida");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            schema = null;
            assumeTrue(false, "Banco inacessível: " + e.getMessage());
        }
        db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"), schema);
        app = Main.createApp(db).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
        }
        if (db != null) {
            db.close();
        }
        if (schema != null) {
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

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private static JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return JSON.readTree(r.body());
    }

    private static void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, JSON.readTree(r.body()).get("error").asText());
    }

    @Test
    void roomsAreListed() throws Exception {
        JsonNode rooms = JSON.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble());
    }

    @Test
    void validationAndAvailability() throws Exception {
        assertError(call("POST", "/reservations", "nope"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(102, "2027-03-10", "2027-03-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("GET", "/reservations/99999", null), 404, "RESERVATION_NOT_FOUND");

        JsonNode first = create(102, "2028-03-10", "2028-03-12", 2, "STANDARD");
        assertEquals("PENDING", first.get("status").asText());
        assertEquals(499.80, first.get("total").asDouble());
        assertTrue(first.get("refund").isNull());
        assertError(call("POST", "/reservations", booking(102, "2028-03-11", "2028-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        // saída no dia da entrada de outra reserva é permitido
        create(102, "2028-03-12", "2028-03-14", 1, "STANDARD");
        // cancelada libera o quarto
        long id = first.get("id").asLong();
        assertEquals(200, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        create(102, "2028-03-10", "2028-03-12", 1, "STANDARD");
    }

    @Test
    void lifecycleNotificationsAndReports() throws Exception {
        int before = JSON.readTree(call("GET", "/messages", null).body()).size();
        JsonNode r = create(201, "2029-03-10", "2029-03-12", 2, "STANDARD");
        long id = r.get("id").asLong();
        String path = "/reservations/" + id;

        assertError(call("GET", path + "/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", path + "/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        // cancelar pendente não avisa ninguém
        JsonNode other = create(202, "2029-03-10", "2029-03-12", 2, "STANDARD");
        call("POST", "/reservations/" + other.get("id").asLong() + "/cancellation", null);
        assertEquals(before, JSON.readTree(call("GET", "/messages", null).body()).size());

        JsonNode confirmed = JSON.readTree(call("POST", path + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(779.80, confirmed.get("amountPaid").asDouble());
        assertError(call("POST", path + "/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode msgs = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(before + 3, msgs.size());
        String all = msgs.toString();
        assertTrue(all.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(all.contains("Reserva confirmada - quarto 201"));
        assertTrue(all.contains("Pagamento recebido - reserva " + id));
        assertTrue(all.contains("governanca@pousadamarealta.com.br"));
        assertTrue(all.contains("financeiro@pousadamarealta.com.br"));

        String csv = call("GET", "/reports/occupancy?date=2029-03-11", null).body();
        assertTrue(csv.startsWith("room,type,reservation,check_in,check_out\n"), csv);
        assertTrue(csv.contains("201,DELUXE," + id + ",2029-03-10,2029-03-12\n"), csv);
        assertEquals(0, call("GET", "/reports/occupancy?date=2029-03-12", null).body().indexOf("room,"));
        assertTrue(call("GET", "/reports/occupancy?date=2029-03-12", null).body().contains("total,0/5"));
        assertError(call("GET", "/reports/occupancy?date=xx", null), 400, "INVALID_REQUEST");

        assertEquals(200, call("POST", path + "/check-in", null).statusCode());
        assertError(call("POST", path + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        HttpResponse<String> out = call("POST", path + "/check-out", null);
        assertEquals(200, out.statusCode());
        JsonNode doc = JSON.readTree(out.body());
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("DELUXE", doc.get("roomType").asText());
        assertEquals(doc, JSON.readTree(call("GET", path + "/document", null).body()));

        String revenue = call("GET", "/reports/revenue?month=2029-03", null).body();
        assertTrue(revenue.contains(id + ",201,2029-03-10,2029-03-12,CHECKED_OUT,779.80\n"), revenue);
        assertError(call("GET", "/reports/revenue?month=2029-3", null), 400, "INVALID_REQUEST");
    }

    @Test
    void cancellationOfPaidReservations() throws Exception {
        // plano normal: devolve tudo e avisa governança e financeiro
        long a = create(301, "2030-03-10", "2030-03-11", 4, "STANDARD").get("id").asLong();
        call("POST", "/reservations/" + a + "/confirmation", null);
        int before = JSON.readTree(call("GET", "/messages", null).body()).size();
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/" + a + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(579.90, cancelled.get("refund").asDouble());
        String msgs = JSON.readTree(call("GET", "/messages", null).body()).toString();
        assertEquals(before + 2, JSON.readTree(call("GET", "/messages", null).body()).size());
        assertTrue(msgs.contains("Reserva cancelada - quarto 301"));
        assertTrue(msgs.contains("Reembolso - reserva " + a));
        assertTrue(msgs.contains("R$ 579,90"));

        // não reembolsável: nada a devolver, só a governança é avisada, e o valor continua no financeiro
        long b = create(301, "2030-04-10", "2030-04-11", 1, "NON_REFUNDABLE").get("id").asLong();
        JsonNode conf = JSON.readTree(call("POST", "/reservations/" + b + "/confirmation", null).body());
        assertEquals(492.92, conf.get("total").asDouble());
        before = JSON.readTree(call("GET", "/messages", null).body()).size();
        JsonNode c = JSON.readTree(call("POST", "/reservations/" + b + "/cancellation", null).body());
        assertEquals(0.0, c.get("refund").asDouble());
        assertEquals(before + 1, JSON.readTree(call("GET", "/messages", null).body()).size());
        String revenue = call("GET", "/reports/revenue?month=2030-04", null).body();
        assertTrue(revenue.contains(b + ",301,2030-04-10,2030-04-11,CANCELLED,492.92\n"), revenue);
    }
}
