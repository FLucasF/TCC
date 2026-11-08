package br.com.pousadamarealta.reservas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import br.com.pousadamarealta.reservas.persistence.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/** Roda contra o PostgreSQL de DB_URL, num schema temporário que some no fim. Sem DB_URL, é ignorado. */
class ApiTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));

    private String schema;
    private Database db;
    private Javalin app;
    private String base;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() throws Exception {
        String url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "DB_URL não definida");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        admin("CREATE SCHEMA " + schema);
        db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"), schema);
        app = App.create(db).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) admin("DROP SCHEMA " + schema + " CASCADE");
    }

    private void admin(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return MAPPER.readTree(r.body());
    }

    private static String body(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", body(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return json(r);
    }

    private void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, json(r).get("error").asText());
        assertTrue(json(r).hasNonNull("message"));
    }

    @Test
    void listsRooms() throws Exception {
        JsonNode rooms = json(call("GET", "/rooms", null));
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
        assertEquals(4, rooms.get(4).get("capacity").asInt());
        assertEquals("249.90", rooms.get(0).get("nightlyRate").decimalValue().toPlainString());
    }

    @Test
    void createsReservationWithHighSeasonPrice() throws Exception {
        JsonNode r = create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");
        HttpResponse<String> raw = call("GET", "/reservations/1", null);
        assertTrue(raw.body().contains("\"total\":599.76") && raw.body().contains("\"discount\":0.00"), raw.body());
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(2, r.get("nights").asInt());
        assertEquals("599.76", r.get("total").decimalValue().toPlainString());
        assertEquals("0.00", r.get("discount").decimalValue().toPlainString());
        assertEquals("0.00", r.get("amountPaid").decimalValue().toPlainString());
        assertTrue(r.get("refund").isNull());
        assertEquals("2027-07-10", r.get("checkIn").asText());
        assertEquals(2, create(102, "2027-03-01", "2027-03-02", 1, "NON_REFUNDABLE").get("id").asInt());
    }

    @Test
    void validatesCreateRequests() throws Exception {
        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", ""), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-07-12", "2027-07-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-07-12", "2027-07-10", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "10/07/2027", "2027-07-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 1, "OUTRO")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(999, "2027-07-10", "2027-07-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
    }

    @Test
    void rejectsOverlappingNightsButAllowsBackToBackAndCancelled() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(call("POST", "/reservations", body(101, "2027-03-11", "2027-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        assertError(call("POST", "/reservations", body(101, "2027-03-09", "2027-03-11", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        create(101, "2027-03-12", "2027-03-14", 1, "STANDARD");
        create(101, "2027-03-08", "2027-03-10", 1, "STANDARD");
        assertEquals(200, call("POST", "/reservations/1/cancellation", null).statusCode());
        create(101, "2027-03-10", "2027-03-12", 1, "STANDARD");
    }

    @Test
    void concurrentBookingsOfSameRoomAllowOnlyOne() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(() -> call("POST", "/reservations", body(201, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        }
        int created = 0;
        for (Future<Integer> f : pool.invokeAll(tasks)) {
            int status = f.get();
            if (status == 201) created++; else assertEquals(409, status);
        }
        pool.shutdown();
        assertEquals(1, created);
    }

    @Test
    void confirmationNotifiesGuestGovernanceAndFinance() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        JsonNode r = json(call("POST", "/reservations/1/confirmation", null));
        assertEquals("CONFIRMED", r.get("status").asText());
        assertEquals("779.80", r.get("amountPaid").decimalValue().toPlainString());

        JsonNode messages = json(call("GET", "/messages", null));
        assertEquals(3, messages.size());
        String receipt = """
                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""";
        JsonNode guest = bySubject(messages, "Reserva confirmada - Pousada Maré Alta");
        assertEquals("EMAIL", guest.get("channel").asText());
        assertEquals("carla@example.com", guest.get("to").asText());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", guest.get("body").asText());

        JsonNode gov = bySubject(messages, "Reserva confirmada - quarto 201");
        assertEquals("governanca@pousadamarealta.com.br", gov.get("to").asText());
        assertTrue(gov.get("body").asText().contains("10/03/2027"));
        assertTrue(gov.get("body").asText().contains("12/03/2027"));
        assertTrue(gov.get("body").asText().contains("2"));

        JsonNode fin = bySubject(messages, "Pagamento recebido - reserva 1");
        assertEquals("financeiro@pousadamarealta.com.br", fin.get("to").asText());
        assertTrue(fin.get("body").asText().contains("R$ 779,80"));

        assertError(call("POST", "/reservations/1/confirmation", null), 409, "OPERATION_NOT_ALLOWED");
        assertEquals(3, json(call("GET", "/messages", null)).size());
    }

    private static JsonNode bySubject(JsonNode messages, String subject) {
        for (JsonNode m : messages) if (m.get("subject").asText().equals(subject)) return m;
        fail("sem mensagem com assunto " + subject);
        return null;
    }

    @Test
    void cancellingPendingNotifiesNobody() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        JsonNode r = json(call("POST", "/reservations/1/cancellation", null));
        assertEquals("CANCELLED", r.get("status").asText());
        assertEquals("0.00", r.get("refund").decimalValue().toPlainString());
        assertEquals(0, json(call("GET", "/messages", null)).size());
    }

    @Test
    void cancellingConfirmedStandardRefundsAndNotifies() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        JsonNode r = json(call("POST", "/reservations/1/cancellation", null));
        assertEquals("779.80", r.get("refund").decimalValue().toPlainString());
        JsonNode messages = json(call("GET", "/messages", null));
        assertEquals(5, messages.size());
        JsonNode gov = bySubject(messages, "Reserva cancelada - quarto 201");
        assertEquals("governanca@pousadamarealta.com.br", gov.get("to").asText());
        assertTrue(gov.get("body").asText().contains("10/03/2027") && gov.get("body").asText().contains("12/03/2027"));
        JsonNode fin = bySubject(messages, "Reembolso - reserva 1");
        assertTrue(fin.get("body").asText().contains("R$ 779,80"));
        assertError(call("POST", "/reservations/1/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
    }

    @Test
    void cancellingConfirmedNonRefundableNotifiesOnlyGovernance() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE");
        JsonNode confirmed = json(call("POST", "/reservations/1/confirmation", null));
        assertEquals("662.83", confirmed.get("amountPaid").decimalValue().toPlainString());
        JsonNode r = json(call("POST", "/reservations/1/cancellation", null));
        assertEquals("0.00", r.get("refund").decimalValue().toPlainString());
        JsonNode messages = json(call("GET", "/messages", null));
        assertEquals(4, messages.size());
        bySubject(messages, "Reserva cancelada - quarto 201");
        for (JsonNode m : messages) assertFalse(m.get("subject").asText().startsWith("Reembolso"));
    }

    @Test
    void lifecycleAndDocument() throws Exception {
        create(201, "2027-03-10", "2027-03-11", 2, "NON_REFUNDABLE");
        assertError(call("GET", "/reservations/1/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations/1/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations/1/check-out", null), 409, "OPERATION_NOT_ALLOWED");
        call("POST", "/reservations/1/confirmation", null);
        assertError(call("POST", "/reservations/1/check-out", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode doc = json(call("GET", "/reservations/1/document", null));
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals(1, doc.get("reservationId").asInt());
        assertEquals("DELUXE", doc.get("roomType").asText());
        assertEquals("58.48", doc.get("discount").decimalValue().toPlainString());
        assertEquals("331.42", doc.get("total").decimalValue().toPlainString());
        assertEquals("CHECKED_IN", json(call("POST", "/reservations/1/check-in", null)).get("status").asText());
        assertError(call("POST", "/reservations/1/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode out = json(call("POST", "/reservations/1/check-out", null));
        assertEquals("Comprovante de reserva", out.get("title").asText());
        assertEquals("CHECKED_OUT", json(call("GET", "/reservations/1", null)).get("status").asText());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void unknownReservation() throws Exception {
        assertError(call("GET", "/reservations/42", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reservations/abc", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("POST", "/reservations/42/confirmation", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reservations/42/document", null), 404, "RESERVATION_NOT_FOUND");
    }

    @Test
    void reports() throws Exception {
        create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");   // 1: confirmada
        create(301, "2027-07-11", "2027-07-13", 2, "STANDARD");   // 2: pendente, não conta
        create(201, "2027-07-09", "2027-07-10", 2, "NON_REFUNDABLE"); // 3: cancelada, não reembolsável
        create(102, "2027-06-30", "2027-07-02", 2, "STANDARD");   // 4: começa em junho
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/3/confirmation", null);
        call("POST", "/reservations/3/cancellation", null);
        call("POST", "/reservations/4/confirmation", null);

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2027-07-10", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD,1,2027-07-10,2027-07-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\n102,STANDARD,4,2027-06-30,2027-07-02\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2027-07-01", null).body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());
        assertError(call("GET", "/reports/occupancy?date=10-07-2027", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/occupancy", null), 400, "INVALID_REQUEST");

        HttpResponse<String> rev = call("GET", "/reports/revenue?month=2027-07", null);
        assertEquals("text/csv; charset=utf-8", rev.headers().firstValue("Content-Type").orElse(""));
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + "3,201,2027-07-09,2027-07-10,CANCELLED,397.70\n"
                + "1,101,2027-07-10,2027-07-12,CONFIRMED,599.76\n"
                + "total,997.46\n", rev.body());
        assertEquals("reservation,room,check_in,check_out,status,amount\n4,102,2027-06-30,2027-07-02,CONFIRMED,549.78\ntotal,549.78\n",
                call("GET", "/reports/revenue?month=2027-06", null).body());
        assertEquals("reservation,room,check_in,check_out,status,amount\ntotal,0.00\n",
                call("GET", "/reports/revenue?month=2027-01", null).body());
        assertError(call("GET", "/reports/revenue?month=2027-7", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
    }
}
