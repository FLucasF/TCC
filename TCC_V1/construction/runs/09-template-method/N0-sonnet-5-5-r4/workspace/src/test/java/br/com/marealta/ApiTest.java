package br.com.marealta;

import br.com.marealta.report.ReportService;
import br.com.marealta.report.TourismTax;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.App;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private final HttpClient client = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        InMemoryStore store = new InMemoryStore();
        app = App.create(new ReservationService(store), new ReportService(store,
                ReportService.standardReports(TourismTax.flat(new BigDecimal("4.50"))))).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = send("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return mapper.readTree(r.body());
    }

    private void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, mapper.readTree(r.body()).get("error").asText());
    }

    @Test
    void roomsAreListed() throws Exception {
        JsonNode rooms = mapper.readTree(send("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals("{\"number\":101,\"type\":\"STANDARD\",\"capacity\":2,\"nightlyRate\":249.90}",
                rooms.get(0).toString());
    }

    @Test
    void fullLifecycleWithReceiptAndEmail() throws Exception {
        JsonNode r = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("0.00", r.get("amountPaid").toString());
        assertTrue(r.get("refund").isNull());
        assertEquals("779.80", r.get("total").toString());

        assertEquals(0, mapper.readTree(send("GET", "/messages", null).body()).size());
        assertError(send("GET", "/reservations/1/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(send("POST", "/reservations/1/check-in", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode confirmed = mapper.readTree(send("POST", "/reservations/1/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("779.80", confirmed.get("amountPaid").toString());
        assertError(send("POST", "/reservations/1/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode msgs = mapper.readTree(send("GET", "/messages", null).body());
        assertEquals(1, msgs.size());
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());
        assertEquals("carla@example.com", msgs.get(0).get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", msgs.get(0).get("body").asText());

        assertEquals("CHECKED_IN", mapper.readTree(send("POST", "/reservations/1/check-in", null).body())
                .get("status").asText());
        assertError(send("POST", "/reservations/1/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode doc = mapper.readTree(send("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("DELUXE", doc.get("roomType").asText());
        assertEquals("2027-03-10", doc.get("checkIn").asText());
        assertEquals("0.00", doc.get("discount").toString());
        assertEquals(doc, mapper.readTree(send("GET", "/reservations/1/document", null).body()));
    }

    @Test
    void cancellationRefunds() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD");   // 1: pending
        create(102, "2027-03-10", "2027-03-11", 1, "STANDARD");   // 2: paid, refundable
        create(201, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE"); // 3: paid, not refundable
        send("POST", "/reservations/2/confirmation", null);
        send("POST", "/reservations/3/confirmation", null);

        assertEquals("0.00", refundOf(1));
        assertEquals("249.90", refundOf(2));
        assertEquals("0.00", refundOf(3));
        // cancelled reservations free the room
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD");
    }

    private String refundOf(int id) throws Exception {
        JsonNode r = mapper.readTree(send("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals("CANCELLED", r.get("status").asText());
        return r.get("refund").toString();
    }

    @Test
    void validationAndConflicts() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(send("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        assertEquals(201, send("POST", "/reservations", booking(101, "2027-03-12", "2027-03-13", 1, "STANDARD")).statusCode());
        assertError(send("POST", "/reservations", booking(101, "2027-04-01", "2027-04-02", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(send("POST", "/reservations", booking(999, "2027-04-01", "2027-04-02", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(send("POST", "/reservations", booking(101, "2027-04-02", "2027-04-02", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", booking(101, "2027-04-01", "2027-04-02", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", booking(101, "2027-04-01", "2027-04-02", 1, "OTHER")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", booking(101, "04/01/2027", "2027-04-02", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", "{\"roomNumber\":101}"), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", "not json"), 400, "INVALID_REQUEST");
        assertError(send("GET", "/reservations/99", null), 404, "RESERVATION_NOT_FOUND");
        assertError(send("POST", "/reservations/99/check-out", null), 404, "RESERVATION_NOT_FOUND");
    }

    @Test
    void reports() throws Exception {
        create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");         // 1 paid
        create(201, "2027-07-11", "2027-07-14", 2, "NON_REFUNDABLE");   // 2 paid, then cancelled
        create(102, "2027-07-11", "2027-07-12", 1, "STANDARD");         // 3 pending
        create(202, "2027-08-01", "2027-08-02", 1, "STANDARD");         // 4 other month
        send("POST", "/reservations/1/confirmation", null);
        send("POST", "/reservations/2/confirmation", null);
        send("POST", "/reservations/2/cancellation", null);
        send("POST", "/reservations/2/confirmation", null);

        HttpResponse<String> occ = send("GET", "/reports/occupancy?date=2027-07-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-12
                total,1/5
                """, occ.body());
        assertTrue(send("GET", "/reports/occupancy?date=2027-07-12", null).body().endsWith("total,0/5\n"));

        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
                2,201,2027-07-11,2027-07-14,CANCELLED,1193.09
                total,1792.85
                """, send("GET", "/reports/revenue?month=2027-07", null).body());
    }
}
