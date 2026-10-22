package br.com.maretalta;

import br.com.maretalta.store.InMemoryStore;
import br.com.maretalta.store.Store;
import br.com.maretalta.web.App;
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
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    protected Store newStore() { return new InMemoryStore(); }

    @BeforeEach
    void start() {
        app = App.create(newStore(), new BigDecimal("4.50")).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception { return MAPPER.readTree(body); }
    }

    Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    long create(int room, String in, String out, int guests, String plan) throws Exception {
        Res r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.status(), r.body());
        return r.json().get("id").asLong();
    }

    @Test
    void rooms() throws Exception {
        JsonNode rooms = call("GET", "/rooms", null).json();
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertEquals(579.90, rooms.get(4).get("nightlyRate").asDouble());
    }

    @Test
    void lifecycleAndReceiptAndEmail() throws Exception {
        Res created = call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.status());
        JsonNode j = created.json();
        assertEquals(1, j.get("id").asInt());
        assertEquals("PENDING", j.get("status").asText());
        assertTrue(j.get("refund").isNull());
        assertEquals("0.00", j.get("amountPaid").toString());
        assertEquals("779.80", j.get("total").toString());

        assertEquals(409, call("GET", "/reservations/1/document", null).status());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).status());

        JsonNode conf = call("POST", "/reservations/1/confirmation", null).json();
        assertEquals("CONFIRMED", conf.get("status").asText());
        assertEquals("779.80", conf.get("amountPaid").toString());
        assertEquals("OPERATION_NOT_ALLOWED", call("POST", "/reservations/1/confirmation", null).json().get("error").asText());

        JsonNode msgs = call("GET", "/messages", null).json();
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

        assertEquals("CHECKED_IN", call("POST", "/reservations/1/check-in", null).json().get("status").asText());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).status());
        JsonNode doc = call("POST", "/reservations/1/check-out", null).json();
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("DELUXE", doc.get("roomType").asText());
        assertEquals(2, doc.get("nights").asInt());
        assertEquals(200, call("GET", "/reservations/1/document", null).status());
        assertEquals(1, call("GET", "/messages", null).json().size());
    }

    @Test
    void discountLineInReceipt() throws Exception {
        long id = create(101, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE");
        call("POST", "/reservations/" + id + "/confirmation", null);
        String body = call("GET", "/messages", null).json().get(0).get("body").asText();
        assertTrue(body.contains("1 diária\nDiárias: R$ 249,90\nDesconto: R$ 37,48\nTotal: R$ 212,42\n"), body);
    }

    @Test
    void cancellationRefunds() throws Exception {
        long a = create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals("0.00", call("POST", "/reservations/" + a + "/cancellation", null).json().get("refund").toString());

        long b = create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/" + b + "/confirmation", null);
        JsonNode cb = call("POST", "/reservations/" + b + "/cancellation", null).json();
        assertEquals("CANCELLED", cb.get("status").asText());
        assertEquals("499.80", cb.get("refund").toString());
        assertEquals("499.80", cb.get("amountPaid").toString());
        assertEquals(409, call("POST", "/reservations/" + b + "/cancellation", null).status());

        long c = create(101, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE");
        call("POST", "/reservations/" + c + "/confirmation", null);
        assertEquals("0.00", call("POST", "/reservations/" + c + "/cancellation", null).json().get("refund").toString());
        assertEquals(409, call("GET", "/reservations/" + c + "/document", null).status());
    }

    @Test
    void errors() throws Exception {
        assertEquals("ROOM_NOT_FOUND", call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).json().get("error").asText());
        assertEquals(404, call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).status());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).status());
        assertEquals("CAPACITY_EXCEEDED", call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).json().get("error").asText());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-10", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 0, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 1, "XYZ")).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-02-30", "2027-03-12", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", "{not json").status());
        assertEquals(400, call("POST", "/reservations", "{}").status());
        assertEquals("RESERVATION_NOT_FOUND", call("GET", "/reservations/42", null).json().get("error").asText());
        assertEquals(404, call("POST", "/reservations/42/confirmation", null).status());
        assertEquals(404, call("GET", "/reservations/abc", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).status());
        assertEquals(400, call("GET", "/reports/revenue", null).status());
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-7-10", null).status());
    }

    @Test
    void overlapRules() throws Exception {
        long a = create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(409, call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")).status());
        assertEquals("ROOM_UNAVAILABLE", call("POST", "/reservations", booking(101, "2027-03-09", "2027-03-11", 1, "STANDARD")).json().get("error").asText());
        create(101, "2027-03-12", "2027-03-14", 2, "STANDARD"); // sai um dia, entra outro
        create(102, "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/" + a + "/cancellation", null);
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD"); // cancelada libera
    }

    @Test
    void reports() throws Exception {
        long a = create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");        // 599.76
        long b = create(201, "2027-07-11", "2027-07-14", 3, "NON_REFUNDABLE");  // pago e cancelado: fica
        long c = create(301, "2027-07-05", "2027-07-06", 1, "STANDARD");        // pendente: fora
        long d = create(102, "2027-07-20", "2027-07-21", 1, "STANDARD");        // cancelada padrão: fora
        for (long id : new long[] {a, b, d}) call("POST", "/reservations/" + id + "/confirmation", null);
        call("POST", "/reservations/" + b + "/cancellation", null);
        call("POST", "/reservations/" + d + "/cancellation", null);
        call("POST", "/reservations/" + a + "/check-in", null);

        Res occ = call("GET", "/reports/occupancy?date=2027-07-11", null);
        assertEquals("text/csv; charset=utf-8", occ.contentType().toLowerCase());
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-12
                total,1/5
                """, occ.body());
        // dia da saída não conta
        assertEquals("Pousada Maré Alta,occupancy,2027-07-12\nroom,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());

        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CHECKED_IN,599.76
                2,201,2027-07-11,2027-07-14,CANCELLED,1193.09
                total,1792.85
                """, call("GET", "/reports/revenue?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                1,101,2027-07-10,2027-07-12,2,2,18.00
                total,18.00
                """, call("GET", "/reports/tourism-tax?month=2027-07", null).body());

        String plans = call("GET", "/reports/rate-plans?month=2027-07", null).body();
        assertTrue(plans.startsWith("Pousada Maré Alta,rate-plans,2027-07\nrate_plan,reservations,amount\nSTANDARD,1,599.76\nNON_REFUNDABLE,1,"), plans);
        assertTrue(plans.contains("\ntotal,2,"), plans);

        assertEquals("""
                Pousada Maré Alta,rate-plans,2020-01
                rate_plan,reservations,amount
                STANDARD,0,0.00
                NON_REFUNDABLE,0,0.00
                total,0,0.00
                """, call("GET", "/reports/rate-plans?month=2020-01", null).body());
        assertNotEquals(c, 0);
    }
}
