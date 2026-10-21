package br.com.marealta;

import br.com.marealta.web.App;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = new App().create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return mapper.readTree(r.body());
    }

    private String body(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com",
                 "guestCpf":"111.222.333-44","checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}"""
                .formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", body(room, in, out, guests, plan));
        assertEquals(201, r.statusCode());
        return json(r);
    }

    private List<JsonNode> messages() throws Exception {
        List<JsonNode> list = new ArrayList<>();
        json(call("GET", "/messages", null)).forEach(list::add);
        return list;
    }

    private void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode());
        assertEquals(code, json(r).get("error").asText());
    }

    @Test
    void listsRooms() throws Exception {
        JsonNode rooms = json(call("GET", "/rooms", null));
        assertEquals(5, rooms.size());
        assertEquals(389.90, rooms.get(2).get("nightlyRate").asDouble());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
    }

    @Test
    void pricesHighSeasonNights() throws Exception {
        JsonNode r = create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");
        assertEquals("599.76", r.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("0.00", r.get("amountPaid").decimalValue().setScale(2).toPlainString());
        assertTrue(r.get("refund").isNull());
    }

    @Test
    void nonRefundableAppliesDiscount() throws Exception {
        JsonNode r = create(201, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE");
        assertEquals("779.80", r.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("116.97", r.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("662.83", r.get("total").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void confirmationSendsThreeEmails() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals("CONFIRMED", json(call("POST", "/reservations/1/confirmation", null)).get("status").asText());
        List<JsonNode> msgs = messages();
        assertEquals(3, msgs.size());
        JsonNode guest = msgs.stream().filter(m -> m.get("to").asText().equals("carla@example.com")).findFirst().orElseThrow();
        assertEquals("Reserva confirmada - Pousada Maré Alta", guest.get("subject").asText());
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
                Até breve!""", guest.get("body").asText());
        assertEquals("EMAIL", guest.get("channel").asText());
        List<String> subjects = msgs.stream().map(m -> m.get("subject").asText()).toList();
        assertTrue(subjects.contains("Reserva confirmada - quarto 201"));
        assertTrue(subjects.contains("Pagamento recebido - reserva 1"));
    }

    @Test
    void cancellationNotifiesOnlyWhenConfirmed() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/cancellation", null);
        assertEquals(0, messages().size());

        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/2/confirmation", null);
        JsonNode cancelled = json(call("POST", "/reservations/2/cancellation", null));
        assertEquals("499.80", cancelled.get("refund").decimalValue().setScale(2).toPlainString());
        List<String> subjects = messages().stream().map(m -> m.get("subject").asText()).toList();
        assertEquals(5, subjects.size());
        assertTrue(subjects.contains("Reserva cancelada - quarto 101"));
        assertTrue(subjects.contains("Reembolso - reserva 2"));
    }

    @Test
    void nonRefundableCancellationDoesNotNotifyFinance() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE");
        call("POST", "/reservations/1/confirmation", null);
        JsonNode cancelled = json(call("POST", "/reservations/1/cancellation", null));
        assertEquals("0.00", cancelled.get("refund").decimalValue().setScale(2).toPlainString());
        List<String> subjects = messages().stream().map(m -> m.get("subject").asText()).toList();
        assertEquals(4, subjects.size());
        assertTrue(subjects.stream().noneMatch(s -> s.startsWith("Reembolso")));
    }

    @Test
    void lifecycleAndDocument() throws Exception {
        create(201, "2027-03-10", "2027-03-11", 1, "STANDARD");
        assertError(call("GET", "/reservations/1/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations/1/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        call("POST", "/reservations/1/confirmation", null);
        assertError(call("POST", "/reservations/1/confirmation", null), 409, "OPERATION_NOT_ALLOWED");
        assertEquals("CHECKED_IN", json(call("POST", "/reservations/1/check-in", null)).get("status").asText());
        assertError(call("POST", "/reservations/1/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode receipt = json(call("POST", "/reservations/1/check-out", null));
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals("389.90", receipt.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals(receipt, json(call("GET", "/reservations/1/document", null)));
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-03-12", "2027-03-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-03-10", "2027-03-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "10/03/2027", "2027-03-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(999, "2027-03-10", "2027-03-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", body(101, "2027-03-10", "2027-03-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("GET", "/reservations/99", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/occupancy?date=ontem", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
    }

    @Test
    void detectsConflictsOnlyForActiveReservations() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(call("POST", "/reservations", body(101, "2027-03-11", "2027-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        create(101, "2027-03-12", "2027-03-14", 2, "STANDARD");
        call("POST", "/reservations/1/cancellation", null);
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
    }

    @Test
    void reports() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        create(101, "2027-03-11", "2027-03-13", 2, "NON_REFUNDABLE");
        create(102, "2027-03-11", "2027-03-13", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/cancellation", null);

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());

        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                1,201,2027-03-10,2027-03-12,CONFIRMED,779.80
                2,101,2027-03-11,2027-03-13,CANCELLED,424.83
                total,1204.63
                """, call("GET", "/reports/revenue?month=2027-03", null).body());
    }
}
