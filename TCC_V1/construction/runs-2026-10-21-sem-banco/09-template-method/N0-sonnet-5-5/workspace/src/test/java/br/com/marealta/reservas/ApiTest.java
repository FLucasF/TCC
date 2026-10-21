package br.com.marealta.reservas;

import br.com.marealta.reservas.web.App;
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
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTest {
    private final ObjectMapper mapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin server;
    private String base;

    @BeforeEach
    void start() {
        server = new App(App.DEFAULT_TOURISM_TAX).create().start(0);
        base = "http://localhost:" + server.port();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return mapper.readTree(r.body());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private int create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return json(r).get("id").asInt();
    }

    @Test
    void listsRooms() throws Exception {
        JsonNode rooms = json(call("GET", "/rooms", null));
        assertEquals(5, rooms.size());
        assertEquals(301, rooms.get(4).get("number").asInt());
        assertEquals("579.90", rooms.get(4).get("nightlyRate").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void highSeasonPricingAndJsonShape() throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        JsonNode j = json(r);
        assertEquals(1, j.get("id").asInt());
        assertEquals("599.76", j.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("0.00", j.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("PENDING", j.get("status").asText());
        assertEquals("0.00", j.get("amountPaid").decimalValue().setScale(2).toPlainString());
        assertTrue(j.get("refund").isNull());
        assertTrue(r.body().contains("599.76"));
    }

    @Test
    void confirmationSendsEmailAndDocument() throws Exception {
        int id = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        JsonNode c = json(call("POST", "/reservations/" + id + "/confirmation", null));
        assertEquals("CONFIRMED", c.get("status").asText());
        assertEquals("779.80", c.get("amountPaid").decimalValue().setScale(2).toPlainString());
        JsonNode msgs = json(call("GET", "/messages", null));
        assertEquals(1, msgs.size());
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        String expected = """
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""";
        assertEquals(expected, msgs.get(0).get("body").asText());
        JsonNode doc = json(call("GET", "/reservations/" + id + "/document", null));
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("DELUXE", doc.get("roomType").asText());
    }

    @Test
    void lifecycleAndErrors() throws Exception {
        int id = create(101, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE");
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-out", null).statusCode());
        call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).statusCode());
        assertEquals("CHECKED_IN", json(call("POST", "/reservations/" + id + "/check-in", null)).get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        JsonNode out = json(call("POST", "/reservations/" + id + "/check-out", null));
        assertEquals("Comprovante de reserva", out.get("title").asText());
        assertEquals("212.42", out.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals("37.48", out.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals("RESERVATION_NOT_FOUND", json(call("GET", "/reservations/abc", null)).get("error").asText());
    }

    @Test
    void validationErrors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao json").statusCode());
        assertEquals(400, call("POST", "/reservations", "{}").statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-10", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 0, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 1, "X")).statusCode());
        assertEquals("ROOM_NOT_FOUND", json(call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-11", 1, "STANDARD"))).get("error").asText());
        HttpResponse<String> cap = call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 3, "STANDARD"));
        assertEquals(422, cap.statusCode());
        assertEquals("CAPACITY_EXCEEDED", json(cap).get("error").asText());
    }

    @Test
    void doubleBookingAndCancellationFreesRoom() throws Exception {
        int id = create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        HttpResponse<String> clash = call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 2, "STANDARD"));
        assertEquals(409, clash.statusCode());
        assertEquals("ROOM_UNAVAILABLE", json(clash).get("error").asText());
        create(101, "2027-03-12", "2027-03-13", 2, "STANDARD");
        JsonNode cancelled = json(call("POST", "/reservations/" + id + "/cancellation", null));
        assertEquals("0.00", cancelled.get("refund").decimalValue().setScale(2).toPlainString());
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
    }

    @Test
    void refundRules() throws Exception {
        int normal = create(101, "2027-03-01", "2027-03-02", 1, "STANDARD");
        call("POST", "/reservations/" + normal + "/confirmation", null);
        assertEquals("249.90", json(call("POST", "/reservations/" + normal + "/cancellation", null)).get("refund").decimalValue().setScale(2).toPlainString());
        int nr = create(102, "2027-03-01", "2027-03-02", 1, "NON_REFUNDABLE");
        call("POST", "/reservations/" + nr + "/confirmation", null);
        JsonNode c = json(call("POST", "/reservations/" + nr + "/cancellation", null));
        assertEquals("0.00", c.get("refund").decimalValue().setScale(2).toPlainString());
        assertEquals("212.42", c.get("amountPaid").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void reports() throws Exception {
        int a = create(201, "2027-07-10", "2027-07-13", 2, "STANDARD");
        int b = create(101, "2027-07-11", "2027-07-12", 1, "NON_REFUNDABLE");
        int c = create(102, "2027-07-09", "2027-07-10", 1, "STANDARD");
        int pending = create(202, "2027-07-10", "2027-07-11", 1, "STANDARD");
        for (int id : new int[]{a, b, c}) {
            call("POST", "/reservations/" + id + "/confirmation", null);
        }
        call("POST", "/reservations/" + b + "/cancellation", null);

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2027-07-10", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("content-type").orElse(""));
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-10
                room,type,reservation,check_in,check_out
                201,DELUXE,1,2027-07-10,2027-07-13
                total,1/5
                """, occ.body());

        // reserva 2 (não reembolsável cancelada) continua contando; a 3 é do dia 9 e também conta
        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                3,102,2027-07-09,2027-07-10,CONFIRMED,299.88
                1,201,2027-07-10,2027-07-13,CONFIRMED,1403.64
                2,101,2027-07-11,2027-07-12,CANCELLED,254.90
                total,1958.42
                """, call("GET", "/reports/revenue?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                3,102,2027-07-09,2027-07-10,1,1,4.50
                1,201,2027-07-10,2027-07-13,2,3,27.00
                total,31.50
                """, call("GET", "/reports/tourism-tax?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,2,1703.52
                NON_REFUNDABLE,1,254.90
                total,3,1958.42
                """, call("GET", "/reports/rate-plans?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2030-01
                rate_plan,reservations,amount
                STANDARD,0,0.00
                NON_REFUNDABLE,0,0.00
                total,0,0.00
                """, call("GET", "/reports/rate-plans?month=2030-01", null).body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy?date=10/07/2027", null).statusCode());
        assertEquals(400, call("GET", "/reports/tourism-tax", null).statusCode());
        assertTrue(pending > 0);
    }
}
