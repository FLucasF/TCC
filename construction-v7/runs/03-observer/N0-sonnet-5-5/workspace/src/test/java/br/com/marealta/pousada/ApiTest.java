package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.pousada.web.Api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {
    private final ObjectMapper mapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = new Api().create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return mapper.readTree(r.body());
    }

    private String reservationBody(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private long create(int room, String in, String out, int guests, String plan) throws Exception {
        var r = call("POST", "/reservations", reservationBody(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return json(r).get("id").asLong();
    }

    private List<JsonNode> messages() throws Exception {
        return StreamSupport.stream(json(call("GET", "/messages", null)).spliterator(), false).toList();
    }

    @Test
    void roomsAndCreate() throws Exception {
        assertEquals(5, json(call("GET", "/rooms", null)).size());
        var r = call("POST", "/reservations", reservationBody(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        JsonNode j = json(r);
        assertEquals(1, j.get("id").asInt());
        assertEquals("599.76", j.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals("PENDING", j.get("status").asText());
        assertTrue(j.get("refund").isNull());
    }

    @Test
    void validationAndConflicts() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(400, call("POST", "/reservations", reservationBody(101, "2027-07-12", "2027-07-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reservationBody(101, "2027-07-10", "2027-07-12", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", reservationBody(999, "2027-07-10", "2027-07-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", reservationBody(101, "2027-07-10", "2027-07-12", 3, "STANDARD")).statusCode());
        create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");
        var clash = call("POST", "/reservations", reservationBody(101, "2027-07-11", "2027-07-13", 2, "STANDARD"));
        assertEquals(409, clash.statusCode());
        assertEquals("ROOM_UNAVAILABLE", json(clash).get("error").asText());
        create(101, "2027-07-12", "2027-07-13", 2, "STANDARD");
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-7-1", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }

    @Test
    void confirmationNotifiesEveryone() throws Exception {
        long id = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        var conf = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, conf.statusCode());
        assertEquals("779.80", json(conf).get("amountPaid").decimalValue().setScale(2).toPlainString());
        var msgs = messages();
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
        assertTrue(msgs.stream().anyMatch(m -> m.get("to").asText().equals("governanca@pousadamarealta.com.br")
                && m.get("subject").asText().equals("Reserva confirmada - quarto 201")));
        assertTrue(msgs.stream().anyMatch(m -> m.get("to").asText().equals("financeiro@pousadamarealta.com.br")
                && m.get("subject").asText().equals("Pagamento recebido - reserva 1")
                && m.get("body").asText().contains("R$ 779,80")));
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).statusCode());
    }

    @Test
    void cancellationRules() throws Exception {
        long pending = create(101, "2027-03-10", "2027-03-12", 1, "STANDARD");
        var c = call("POST", "/reservations/" + pending + "/cancellation", null);
        assertEquals("0.00", json(c).get("refund").decimalValue().setScale(2).toPlainString());
        assertEquals(0, messages().size());

        long refundable = create(101, "2027-03-10", "2027-03-12", 1, "STANDARD");
        call("POST", "/reservations/" + refundable + "/confirmation", null);
        var c2 = json(call("POST", "/reservations/" + refundable + "/cancellation", null));
        assertEquals("499.80", c2.get("refund").decimalValue().setScale(2).toPlainString());
        var msgs = messages();
        assertEquals(5, msgs.size());
        assertTrue(msgs.stream().anyMatch(m -> m.get("subject").asText().equals("Reembolso - reserva " + refundable)));
        assertTrue(msgs.stream().anyMatch(m -> m.get("subject").asText().equals("Reserva cancelada - quarto 101")));

        long nonRef = create(101, "2027-03-10", "2027-03-12", 1, "NON_REFUNDABLE");
        call("POST", "/reservations/" + nonRef + "/confirmation", null);
        int before = messages().size();
        var c3 = json(call("POST", "/reservations/" + nonRef + "/cancellation", null));
        assertEquals("0.00", c3.get("refund").decimalValue().setScale(2).toPlainString());
        assertEquals(before + 1, messages().size()); // only governance is told: nothing to refund
        assertEquals(409, call("POST", "/reservations/" + nonRef + "/cancellation", null).statusCode());
        // the cancelled reservation frees the room
        create(101, "2027-03-10", "2027-03-12", 1, "STANDARD");
    }

    @Test
    void lifecycleDocumentAndReports() throws Exception {
        long id = create(201, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE");
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-out", null).statusCode());
        assertEquals("CHECKED_IN", json(call("POST", "/reservations/" + id + "/check-in", null)).get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        JsonNode receipt = json(call("POST", "/reservations/" + id + "/check-out", null));
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals("935.76", receipt.get("subtotal").decimalValue().setScale(2).toPlainString());

        var occ = call("GET", "/reports/occupancy?date=2027-07-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-07-10,2027-07-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-07-10,2027-07-12,CHECKED_OUT,795.40\ntotal,795.40\n",
                call("GET", "/reports/revenue?month=2027-07", null).body());
    }
}
