package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Roda contra o PostgreSQL de DB_URL, num schema próprio; é pulado se DB_URL não existir. */
class ApiTest {
    static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String baseUrl, rawUrl, schema, user, password;
    static Database db;
    static Javalin app;

    @BeforeAll
    static void start() throws Exception {
        rawUrl = System.getenv("DB_URL");
        assumeTrue(rawUrl != null, "DB_URL não definido");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(rawUrl, user, password); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        db = new Database(rawUrl + (rawUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        app = App.create(db).start(0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(rawUrl, user, password); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    record Res(int status, String contentType, String body) {
        JsonNode json() throws Exception { return JSON.readTree(body); }
    }

    static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.headers().firstValue("content-type").orElse(""), r.body());
    }

    static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    static long create(int room, String in, String out, int guests, String plan) throws Exception {
        Res r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.status(), r.body());
        return r.json().get("id").asLong();
    }

    static void assertError(Res r, int status, String code) throws Exception {
        assertEquals(status, r.status(), r.body());
        assertEquals(code, r.json().get("error").asText());
    }

    @Test
    void listsRooms() throws Exception {
        JsonNode rooms = call("GET", "/rooms", null).json();
        assertEquals(5, rooms.size());
        assertEquals(301, rooms.get(4).get("number").asInt());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
        assertEquals(4, rooms.get(4).get("capacity").asInt());
        assertEquals("579.90", rooms.get(4).get("nightlyRate").decimalValue().toPlainString());
    }

    @Test
    void validatesRequests() throws Exception {
        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2030-05-10", "2030-05-10", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2030-05-10", "2030-05-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "10/05/2030", "2030-05-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2030-05-10", "2030-05-12", 1, "VIP")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(999, "2030-05-10", "2030-05-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(101, "2030-05-10", "2030-05-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("GET", "/reservations/999999", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reservations/abc", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/occupancy?date=2030-13-01", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue?month=2030-7", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
    }

    @Test
    void fullLifecycleWithNotificationsAndReports() throws Exception {
        int before = call("GET", "/messages", null).json().size();
        Res created = call("POST", "/reservations", booking(201, "2031-03-10", "2031-03-12", 2, "STANDARD"));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("779.80", r.get("subtotal").decimalValue().toPlainString());
        assertEquals("0.00", r.get("amountPaid").decimalValue().toPlainString());
        assertTrue(r.get("refund").isNull());

        assertError(call("POST", "/reservations/" + id + "/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("GET", "/reservations/" + id + "/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations", booking(201, "2031-03-11", "2031-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        assertEquals(201, call("POST", "/reservations", booking(201, "2031-03-12", "2031-03-13", 1, "STANDARD")).status());

        JsonNode confirmed = call("POST", "/reservations/" + id + "/confirmation", null).json();
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("779.80", confirmed.get("amountPaid").decimalValue().toPlainString());
        assertError(call("POST", "/reservations/" + id + "/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode messages = call("GET", "/messages", null).json();
        assertEquals(before + 3, messages.size());
        String subjects = messages.toString();
        assertTrue(subjects.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(subjects.contains("Reserva confirmada - quarto 201"));
        assertTrue(subjects.contains("Pagamento recebido - reserva " + id));
        assertTrue(subjects.contains("governanca@pousadamarealta.com.br"));
        assertTrue(subjects.contains("financeiro@pousadamarealta.com.br"));
        assertEquals("EMAIL", messages.get(before).get("channel").asText());

        String occupancy = call("GET", "/reports/occupancy?date=2031-03-11", null).body();
        assertTrue(occupancy.startsWith("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2031-03-10,2031-03-12\n"), occupancy);
        assertTrue(occupancy.endsWith("total,1/5\n"), occupancy);
        assertTrue(call("GET", "/reports/occupancy?date=2031-03-12", null).body().contains("201,DELUXE") == false);
        assertTrue(call("GET", "/reports/occupancy?date=2031-03-11", null).contentType().toLowerCase().contains("text/csv; charset=utf-8"));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        assertError(call("POST", "/reservations/" + id + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals(200, out.status());
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals("DELUXE", out.json().get("roomType").asText());
        assertEquals("0.00", out.json().get("discount").decimalValue().toPlainString());
        assertEquals(out.json(), call("GET", "/reservations/" + id + "/document", null).json());
    }

    @Test
    void cancellationRulesAndRevenue() throws Exception {
        // pendente: cancela sem avisar ninguém
        int before = call("GET", "/messages", null).json().size();
        long pending = create(101, "2032-03-10", "2032-03-12", 2, "STANDARD");
        JsonNode c = call("POST", "/reservations/" + pending + "/cancellation", null).json();
        assertEquals("CANCELLED", c.get("status").asText());
        assertEquals("0.00", c.get("refund").decimalValue().toPlainString());
        assertEquals(before, call("GET", "/messages", null).json().size());
        // quarto liberado
        long again = create(101, "2032-03-10", "2032-03-12", 2, "STANDARD");

        // normal confirmada: reembolso total e aviso ao financeiro
        call("POST", "/reservations/" + again + "/confirmation", null);
        int mid = call("GET", "/messages", null).json().size();
        JsonNode refunded = call("POST", "/reservations/" + again + "/cancellation", null).json();
        assertEquals("499.80", refunded.get("refund").decimalValue().toPlainString());
        JsonNode msgs = call("GET", "/messages", null).json();
        assertEquals(mid + 2, msgs.size());
        assertTrue(msgs.toString().contains("Reembolso - reserva " + again));
        assertTrue(msgs.toString().contains("Reserva cancelada - quarto 101"));

        // não reembolsável confirmada: sem reembolso, só a governança, e continua na receita
        long nr = create(102, "2032-03-20", "2032-03-22", 2, "NON_REFUNDABLE");
        call("POST", "/reservations/" + nr + "/confirmation", null);
        mid = call("GET", "/messages", null).json().size();
        JsonNode kept = call("POST", "/reservations/" + nr + "/cancellation", null).json();
        assertEquals("0.00", kept.get("refund").decimalValue().toPlainString());
        assertEquals(mid + 1, call("GET", "/messages", null).json().size());

        String revenue = call("GET", "/reports/revenue?month=2032-03", null).body();
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + nr + ",102,2032-03-20,2032-03-22,CANCELLED,424.83\n"
                + "total,424.83\n", revenue);
    }
}
