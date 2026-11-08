package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.report.Reports;
import br.com.marealta.service.ReservationService;
import br.com.marealta.store.Store;
import br.com.marealta.web.WebApp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Testa o sistema inteiro pelo HTTP; as subclasses escolhem o armazenamento. */
abstract class ApiTestBase {
    static final ObjectMapper JSON = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    abstract Store newStore();

    void closeStore() {}

    @BeforeEach
    void start() {
        Store store = newStore();
        app = WebApp.create(new ReservationService(store), Reports.all(store, new BigDecimal("4.50")));
        app.start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
        closeStore();
    }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception { return JSON.readTree(body); }
    }

    Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody());
        else b.method(method, HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
        HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("Content-Type").orElse(""));
    }

    static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com",
                 "guestCpf":"111.222.333-44","checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}"""
                .formatted(room, in, out, guests, plan);
    }

    long create(int room, String in, String out, int guests, String plan) throws Exception {
        Res r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.status(), r.body());
        return r.json().get("id").asLong();
    }

    void assertError(Res r, int status, String code) throws Exception {
        assertEquals(status, r.status(), r.body());
        assertEquals(code, r.json().get("error").asText());
    }

    @Test
    void roomsAreListed() throws Exception {
        Res r = call("GET", "/rooms", null);
        assertEquals(200, r.status());
        assertEquals(5, r.json().size());
        assertEquals(101, r.json().get(0).get("number").asInt());
        assertTrue(r.body().contains("\"nightlyRate\":249.90"), r.body());
        assertTrue(r.body().contains("\"nightlyRate\":579.90"), r.body());
    }

    @Test
    void createReturnsPendingReservation() throws Exception {
        Res r = call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.status());
        assertTrue(r.body().contains("\"id\":1,"), r.body());
        assertTrue(r.body().contains("\"total\":599.76"), r.body());
        assertTrue(r.body().contains("\"amountPaid\":0.00"), r.body());
        assertTrue(r.body().contains("\"refund\":null"), r.body());
        assertEquals("PENDING", r.json().get("status").asText());
        assertEquals(2, r.json().get("nights").asInt());
    }

    @Test
    void validationErrors() throws Exception {
        assertError(call("POST", "/reservations", "nao e json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-07-12", "2027-07-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-02-30", "2027-07-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 1, "XYZ")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(999, "2027-07-10", "2027-07-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("GET", "/reservations/99", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/revenue?month=2027-13", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/occupancy", null), 400, "INVALID_REQUEST");
    }

    @Test
    void overlapIgnoresCancelledAndAllowsBackToBack() throws Exception {
        long first = create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        create(101, "2027-03-12", "2027-03-14", 2, "STANDARD");
        assertEquals(200, call("POST", "/reservations/" + first + "/cancellation", null).status());
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
    }

    @Test
    void confirmationSendsEmailAndReceipt() throws Exception {
        long id = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        Res c = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, c.status());
        assertEquals("CONFIRMED", c.json().get("status").asText());
        assertTrue(c.body().contains("\"amountPaid\":779.80"), c.body());
        assertError(call("POST", "/reservations/" + id + "/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        String receipt = """
                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""";
        JsonNode messages = call("GET", "/messages", null).json();
        assertEquals(1, messages.size());
        JsonNode m = messages.get(0);
        assertEquals("EMAIL", m.get("channel").asText());
        assertEquals("carla@example.com", m.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.get("subject").asText());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. "
                + "Estes são os dados dela:\n\n" + receipt
                + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", m.get("body").asText());
    }

    @Test
    void receiptShowsDiscountAndSingularNight() throws Exception {
        long id = create(102, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE");
        call("POST", "/reservations/" + id + "/confirmation", null);
        String body = call("GET", "/messages", null).json().get(0).get("body").asText();
        assertTrue(body.contains("| 1 diária\nDiárias: R$ 249,90\nDesconto: R$ 37,48\nTotal: R$ 212,42\n"), body);
    }

    @Test
    void lifecycleAndDocument() throws Exception {
        long id = create(301, "2027-03-10", "2027-03-12", 4, "STANDARD");
        String p = "/reservations/" + id;
        assertError(call("GET", p + "/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", p + "/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        call("POST", p + "/confirmation", null);
        assertError(call("POST", p + "/check-out", null), 409, "OPERATION_NOT_ALLOWED");
        assertEquals("CHECKED_IN", call("POST", p + "/check-in", null).json().get("status").asText());
        assertError(call("POST", p + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        Res out = call("POST", p + "/check-out", null);
        assertEquals(200, out.status());
        JsonNode doc = out.json();
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("SUITE", doc.get("roomType").asText());
        assertEquals(301, doc.get("roomNumber").asInt());
        assertTrue(out.body().contains("\"total\":1159.80"), out.body());
        assertEquals(doc, call("GET", p + "/document", null).json());
        assertEquals("CHECKED_OUT", call("GET", p, null).json().get("status").asText());
    }

    @Test
    void refundRules() throws Exception {
        long pending = create(101, "2027-03-10", "2027-03-12", 1, "STANDARD");
        Res r = call("POST", "/reservations/" + pending + "/cancellation", null);
        assertTrue(r.body().contains("\"refund\":0.00"), r.body());

        long standard = create(101, "2027-03-10", "2027-03-12", 1, "STANDARD");
        call("POST", "/reservations/" + standard + "/confirmation", null);
        r = call("POST", "/reservations/" + standard + "/cancellation", null);
        assertTrue(r.body().contains("\"refund\":499.80"), r.body());
        assertTrue(r.body().contains("\"amountPaid\":499.80"), r.body());
        assertError(call("POST", "/reservations/" + standard + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");

        long nonRef = create(101, "2027-03-10", "2027-03-12", 1, "NON_REFUNDABLE");
        call("POST", "/reservations/" + nonRef + "/confirmation", null);
        r = call("POST", "/reservations/" + nonRef + "/cancellation", null);
        assertTrue(r.body().contains("\"refund\":0.00"), r.body());
        assertError(call("GET", "/reservations/" + nonRef + "/document", null), 409, "OPERATION_NOT_ALLOWED");
    }

    /** Monta julho/2027: duas pagas, uma pendente, uma cancelada reembolsada, uma não reembolsável cancelada. */
    private void julyScenario() throws Exception {
        long a = create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");        // 599.76
        long b = create(201, "2027-07-11", "2027-07-14", 2, "NON_REFUNDABLE");  // 3 x 467.88 = 1403.64 - 210.55
        create(102, "2027-07-05", "2027-07-06", 1, "STANDARD");                 // pendente
        long d = create(202, "2027-07-02", "2027-07-04", 1, "STANDARD");
        long e = create(301, "2027-07-20", "2027-07-21", 4, "NON_REFUNDABLE");
        for (long id : new long[] {a, b, d, e}) call("POST", "/reservations/" + id + "/confirmation", null);
        call("POST", "/reservations/" + d + "/cancellation", null);
        call("POST", "/reservations/" + e + "/cancellation", null);
        call("POST", "/reservations/" + a + "/check-in", null);
    }

    @Test
    void occupancyReport() throws Exception {
        julyScenario();
        Res r = call("GET", "/reports/occupancy?date=2027-07-11", null);
        assertEquals("text/csv; charset=utf-8", r.contentType().toLowerCase());
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-12
                201,DELUXE,2,2027-07-11,2027-07-14
                total,2/5
                """, r.body());
        // quem sai no dia não conta
        assertTrue(call("GET", "/reports/occupancy?date=2027-07-12", null).body().endsWith("201,DELUXE,2,2027-07-11,2027-07-14\ntotal,1/5\n"));
    }

    @Test
    void revenueReport() throws Exception {
        julyScenario();
        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CHECKED_IN,599.76
                2,201,2027-07-11,2027-07-14,CONFIRMED,1193.09
                5,301,2027-07-20,2027-07-21,CANCELLED,591.50
                total,2384.35
                """, call("GET", "/reports/revenue?month=2027-07", null).body());
    }

    @Test
    void tourismTaxReport() throws Exception {
        julyScenario();
        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                1,101,2027-07-10,2027-07-12,2,2,18.00
                2,201,2027-07-11,2027-07-14,2,3,27.00
                total,45.00
                """, call("GET", "/reports/tourism-tax?month=2027-07", null).body());
    }

    @Test
    void ratePlansReportListsBothPlansEvenWhenEmpty() throws Exception {
        julyScenario();
        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,1,599.76
                NON_REFUNDABLE,2,1784.59
                total,3,2384.35
                """, call("GET", "/reports/rate-plans?month=2027-07", null).body());
        assertEquals("""
                Pousada Maré Alta,rate-plans,2030-01
                rate_plan,reservations,amount
                STANDARD,0,0.00
                NON_REFUNDABLE,0,0.00
                total,0,0.00
                """, call("GET", "/reports/rate-plans?month=2030-01", null).body());
    }
}
