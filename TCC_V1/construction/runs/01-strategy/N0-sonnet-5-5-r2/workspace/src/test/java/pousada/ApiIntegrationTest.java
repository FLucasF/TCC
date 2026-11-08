package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

/** Sobe a API de verdade contra o PostgreSQL (DB_URL), num schema descartável. */
class ApiIntegrationTest {
    private static final ObjectMapper M = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static String url, user, password;
    private static HikariDataSource ds;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        assumeTrue(url != null, "DB_URL não definida: teste de integração ignorado");
        schema = "it_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, password); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        ds = Db.connect(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        Db.migrate(ds);
        app = Api.create(new ReservationService(ds)).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (ds != null) ds.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, password); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody());
        else b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode json(HttpResponse<String> r) throws Exception { return M.readTree(r.body()); }

    private static String booking(int room, String in, String out, int guests, String plan, String company) {
        return """
            {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
             "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"%s}"""
                .formatted(room, in, out, guests, plan, company == null ? "" : ",\"companyCode\":\"" + company + "\"");
    }

    private static JsonNode create(int room, String in, String out, int guests, String plan, String company) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(room, in, out, guests, plan, company));
        assertEquals(201, r.statusCode(), r.body());
        return json(r);
    }

    private static void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, json(r).get("error").asText());
    }

    @Test
    void rooms() throws Exception {
        JsonNode rooms = json(call("GET", "/rooms", null));
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
        assertEquals(579.90, rooms.get(4).get("nightlyRate").asDouble());
    }

    @Test
    void fullLifecycleWithReceiptAndEmail() throws Exception {
        JsonNode r = create(201, "2031-03-10", "2031-03-12", 2, "STANDARD", null);
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("779.80", r.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals("0.00", r.get("amountPaid").decimalValue().setScale(2).toPlainString());
        assertTrue(r.get("refund").isNull());

        assertError(call("GET", "/reservations/" + id + "/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations/" + id + "/check-in", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode c = json(call("POST", "/reservations/" + id + "/confirmation", null));
        assertEquals("CONFIRMED", c.get("status").asText());
        assertEquals("779.80", c.get("amountPaid").decimalValue().setScale(2).toPlainString());
        assertError(call("POST", "/reservations/" + id + "/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode msgs = json(call("GET", "/messages", null));
        JsonNode m = msgs.get(msgs.size() - 1);
        assertEquals("EMAIL", m.get("channel").asText());
        assertEquals("carla@example.com", m.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.get("subject").asText());
        String receipt = "Comprovante de reserva nº " + id + "\n"
                + "Hóspede: Carla Dias (CPF 111.222.333-44)\n"
                + "Quarto: 201 (DELUXE)\n"
                + "Entrada: 10/03/2031 | Saída: 12/03/2031 | 2 diárias\n"
                + "Diárias: R$ 779,80\n"
                + "Total: R$ 779,80";
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", m.get("body").asText());

        assertEquals("CHECKED_IN", json(call("POST", "/reservations/" + id + "/check-in", null)).get("status").asText());
        assertError(call("POST", "/reservations/" + id + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode doc = json(call("POST", "/reservations/" + id + "/check-out", null));
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals(id, doc.get("reservationId").asLong());
        assertEquals("DELUXE", doc.get("roomType").asText());
        assertEquals("779.80", doc.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals(doc, json(call("GET", "/reservations/" + id + "/document", null)));
        assertEquals("CHECKED_OUT", json(call("GET", "/reservations/" + id, null)).get("status").asText());
    }

    @Test
    void availabilityAndCancellationFreesRoom() throws Exception {
        JsonNode a = create(101, "2031-05-10", "2031-05-12", 2, "NON_REFUNDABLE", null);
        assertEquals("2031-05-10", a.get("checkIn").asText());
        assertError(call("POST", "/reservations", booking(101, "2031-05-11", "2031-05-13", 1, "STANDARD", null)),
                409, "ROOM_UNAVAILABLE");
        // saída no dia da entrada de outra não conflita
        create(101, "2031-05-12", "2031-05-13", 1, "STANDARD", null);
        long id = a.get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode cancelled = json(call("POST", "/reservations/" + id + "/cancellation", null));
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals("0.00", cancelled.get("refund").decimalValue().setScale(2).toPlainString());
        assertEquals("424.83", cancelled.get("amountPaid").decimalValue().setScale(2).toPlainString()); // 499.80 - 15%
        create(101, "2031-05-11", "2031-05-12", 1, "STANDARD", null);
    }

    @Test
    void pendingCancellationRefundsNothing() throws Exception {
        long id = create(102, "2031-06-10", "2031-06-11", 1, "STANDARD", null).get("id").asLong();
        JsonNode c = json(call("POST", "/reservations/" + id + "/cancellation", null));
        assertEquals("0.00", c.get("refund").decimalValue().setScale(2).toPlainString());
        assertEquals("0.00", c.get("amountPaid").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void validationErrors() throws Exception {
        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2031-05-10", "2031-05-10", 1, "STANDARD", null)), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2031-05-10", "2031-05-11", 0, "STANDARD", null)), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "10/05/2031", "2031-05-11", 1, "STANDARD", null)), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2031-05-10", "2031-05-11", 1, "NOPE", null)), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(999, "2031-05-10", "2031-05-11", 1, "STANDARD", null)), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(101, "2031-05-10", "2031-05-11", 3, "STANDARD", null)), 422, "CAPACITY_EXCEEDED");
        assertError(call("GET", "/reservations/99999", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reservations/abc", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/occupancy?date=2031-13-01", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/occupancy", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue?month=2031-7", null), 400, "INVALID_REQUEST");
    }

    @Test
    void planRules() throws Exception {
        assertError(call("POST", "/reservations", booking(201, "2031-03-10", "2031-03-12", 2, "CORPORATE", null)), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(201, "2031-03-10", "2031-03-12", 2, "CORPORATE", "XPTO")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(201, "2031-03-10", "2031-03-12", 2, "LONG_STAY", null)), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(201, "2031-03-10", "2031-03-12", 2, "WEEKEND_PACKAGE", null)), 400, "INVALID_REQUEST");

        JsonNode corp = create(201, "2031-09-10", "2031-09-12", 2, "CORPORATE", "TECNOSUL");
        assertEquals("93.58", corp.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("686.22", corp.get("total").decimalValue().setScale(2).toPlainString());
        // companyCode é ignorado fora do convênio
        assertEquals("779.80", create(202, "2031-09-10", "2031-09-12", 2, "STANDARD", "XPTO").get("total").decimalValue().setScale(2).toPlainString());

        // 2031-03-14 é sexta
        JsonNode pkg = create(201, "2031-03-14", "2031-03-16", 2, "WEEKEND_PACKAGE", null);
        assertEquals("820.00", pkg.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("0.00", pkg.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals(2, pkg.get("nights").asInt());
        long id = pkg.get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("410.00", json(call("POST", "/reservations/" + id + "/cancellation", null)).get("refund").decimalValue().setScale(2).toPlainString());

        JsonNode ls = create(101, "2031-10-01", "2031-10-08", 2, "LONG_STAY", null);
        assertEquals("1749.30", ls.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("174.93", ls.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("1574.37", ls.get("total").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void reports() throws Exception {
        long a = create(301, "2032-07-10", "2032-07-12", 4, "STANDARD", null).get("id").asLong();
        long b = create(101, "2032-07-11", "2032-07-13", 2, "NON_REFUNDABLE", null).get("id").asLong();
        long pending = create(202, "2032-07-10", "2032-07-12", 2, "STANDARD", null).get("id").asLong();
        call("POST", "/reservations/" + a + "/confirmation", null);
        call("POST", "/reservations/" + b + "/confirmation", null);
        call("POST", "/reservations/" + b + "/cancellation", null);

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2032-07-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("room,type,reservation,check_in,check_out\n301,SUITE," + a + ",2032-07-10,2032-07-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2032-07-12", null).body());

        String rev = call("GET", "/reports/revenue?month=2032-07", null).body();
        // a: 2 noites de suíte em julho = 2*695.88 = 1391.76; b: 2*299.88=599.76 -15% = 509.80, pago e não devolvido
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + a + ",301,2032-07-10,2032-07-12,CONFIRMED,1391.76\n"
                + b + ",101,2032-07-11,2032-07-13,CANCELLED,509.80\n"
                + "total,1901.56\n", rev);
        assertFalse(rev.contains(pending + ",202"));
    }
}
