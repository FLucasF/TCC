package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.store.MemoryStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = App.create(new MemoryStore(Config.initialRooms())).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void moneyIsWrittenWithTwoDecimals() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD");
        String body = send("GET", "/reservations/1", null).body();
        assertTrue(body.contains("\"amountPaid\":0.00"), body);
        assertTrue(body.contains("\"total\":249.90"), body);
        assertTrue(body.contains("\"refund\":null"), body);
        assertTrue(body.contains("\"checkIn\":\"2027-03-10\""), body);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = send("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return mapper.readTree(r.body());
    }

    @Test
    void roomsAreListed() throws Exception {
        JsonNode rooms = mapper.readTree(send("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble());
    }

    @Test
    void fullLifecycleWithDocumentAndEmail() throws Exception {
        JsonNode r = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(0, r.get("amountPaid").decimalValue().signum());
        assertTrue(r.get("refund").isNull());
        assertEquals(0, new java.math.BigDecimal("779.80").compareTo(r.get("total").decimalValue()));

        assertEquals(409, send("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, send("POST", "/reservations/1/check-in", null).statusCode());

        JsonNode confirmed = mapper.readTree(send("POST", "/reservations/1/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(0, new java.math.BigDecimal("779.80").compareTo(confirmed.get("amountPaid").decimalValue()));
        assertEquals(409, send("POST", "/reservations/1/confirmation", null).statusCode());

        JsonNode messages = mapper.readTree(send("GET", "/messages", null).body());
        assertEquals(1, messages.size());
        assertEquals("EMAIL", messages.get(0).get("channel").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
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
                Até breve!""", messages.get(0).get("body").asText());

        assertEquals(200, send("GET", "/reservations/1/document", null).statusCode());
        assertEquals("CHECKED_IN", mapper.readTree(send("POST", "/reservations/1/check-in", null).body())
                .get("status").asText());
        assertEquals(409, send("POST", "/reservations/1/cancellation", null).statusCode());
        JsonNode doc = mapper.readTree(send("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("DELUXE", doc.get("roomType").asText());
        assertEquals(0, doc.get("discount").decimalValue().signum());
        assertEquals(1, mapper.readTree(send("GET", "/messages", null).body()).size());
    }

    @Test
    void cancellationRefunds() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD");
        create(102, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE");
        create(201, "2027-03-10", "2027-03-11", 1, "STANDARD");
        send("POST", "/reservations/1/confirmation", null);
        send("POST", "/reservations/2/confirmation", null);

        assertEquals(0, new java.math.BigDecimal("249.90").compareTo(
                mapper.readTree(send("POST", "/reservations/1/cancellation", null).body())
                        .get("refund").decimalValue()));
        assertEquals(0, mapper.readTree(send("POST", "/reservations/2/cancellation", null).body())
                .get("refund").decimalValue().signum());
        JsonNode pending = mapper.readTree(send("POST", "/reservations/3/cancellation", null).body());
        assertEquals(0, pending.get("refund").decimalValue().signum());
        assertEquals(409, send("POST", "/reservations/3/cancellation", null).statusCode());
        // quarto liberado pelo cancelamento
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD");
    }

    @Test
    void validationAndConflicts() throws Exception {
        assertEquals(400, send("POST", "/reservations", "não é json").statusCode());
        assertEquals(400, send("POST", "/reservations", "{}").statusCode());
        assertEquals(400, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-10", 1, "STANDARD")).statusCode());
        assertEquals(400, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 0, "STANDARD")).statusCode());
        assertEquals(400, send("POST", "/reservations", booking(101, "10/03/2027", "2027-03-11", 1, "STANDARD")).statusCode());
        assertEquals(400, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 1, "XYZ")).statusCode());
        HttpResponse<String> notFound = send("POST", "/reservations", booking(999, "2027-03-10", "2027-03-11", 1, "STANDARD"));
        assertEquals(404, notFound.statusCode());
        assertEquals("ROOM_NOT_FOUND", mapper.readTree(notFound.body()).get("error").asText());
        assertEquals(422, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 3, "STANDARD")).statusCode());

        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(409, send("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")).statusCode());
        create(101, "2027-03-12", "2027-03-13", 1, "STANDARD"); // sai e entra no mesmo dia

        assertEquals(404, send("GET", "/reservations/99", null).statusCode());
        assertEquals(404, send("GET", "/reservations/abc", null).statusCode());
        assertEquals(400, send("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, send("GET", "/reports/occupancy", null).statusCode());
    }

    private void paidFlow(int room, String in, String out, int guests, String plan) throws Exception {
        JsonNode r = create(room, in, out, guests, plan);
        send("POST", "/reservations/" + r.get("id").asInt() + "/confirmation", null);
    }

    @Test
    void reports() throws Exception {
        paidFlow(101, "2027-07-10", "2027-07-12", 2, "STANDARD");        // 1: 599.76
        paidFlow(201, "2027-07-11", "2027-07-14", 2, "NON_REFUNDABLE");  // 2
        create(102, "2027-07-11", "2027-07-12", 1, "STANDARD");          // 3: pendente
        paidFlow(301, "2027-07-20", "2027-07-21", 1, "NON_REFUNDABLE");  // 4
        send("POST", "/reservations/4/cancellation", null);              // fica com a pousada
        paidFlow(202, "2027-07-22", "2027-07-23", 1, "STANDARD");        // 5
        send("POST", "/reservations/5/cancellation", null);              // devolvido

        HttpResponse<String> occ = send("GET", "/reports/occupancy?date=2027-07-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-12
                201,DELUXE,2,2027-07-11,2027-07-14
                total,2/5
                """, occ.body());
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-12
                room,type,reservation,check_in,check_out
                201,DELUXE,2,2027-07-11,2027-07-14
                total,1/5
                """, send("GET", "/reports/occupancy?date=2027-07-12", null).body());

        // 2: 389.90*1.2=467.88 *3 = 1403.64; desconto 210.546 -> 210.55; total 1193.09
        // 4: 579.90*1.2=695.88; desconto 104.382 -> 104.38; total 591.50
        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
                2,201,2027-07-11,2027-07-14,CONFIRMED,1193.09
                4,301,2027-07-20,2027-07-21,CANCELLED,591.50
                total,2384.35
                """, send("GET", "/reports/revenue?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                1,101,2027-07-10,2027-07-12,2,2,18.00
                2,201,2027-07-11,2027-07-14,2,3,27.00
                total,45.00
                """, send("GET", "/reports/tourism-tax?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,1,599.76
                NON_REFUNDABLE,2,1784.59
                total,3,2384.35
                """, send("GET", "/reports/rate-plans?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-08
                rate_plan,reservations,amount
                STANDARD,0,0.00
                NON_REFUNDABLE,0,0.00
                total,0,0.00
                """, send("GET", "/reports/rate-plans?month=2027-08", null).body());
    }
}
