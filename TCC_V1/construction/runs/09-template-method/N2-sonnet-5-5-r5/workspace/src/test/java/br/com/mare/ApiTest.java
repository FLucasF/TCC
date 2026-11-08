package br.com.mare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.mare.domain.Money;
import br.com.mare.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {
    private final ObjectMapper mapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        var ds = Database.connect("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", null, null);
        app = Main.build(ds, Money.of("4.50")).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        var r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return mapper.readTree(r.body());
    }

    @Test
    void highSeasonPricing() throws Exception {
        JsonNode r = create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");
        assertEquals(1, r.get("id").asInt());
        assertEquals("599.76", r.get("total").asText());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("refund").isNull());
    }

    @Test
    void confirmSendsEmailAndReceipt() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        var c = call("POST", "/reservations/1/confirmation", null);
        assertEquals(200, c.statusCode());
        assertEquals("779.80", mapper.readTree(c.body()).get("amountPaid").asText());
        JsonNode msgs = mapper.readTree(call("GET", "/messages", null).body());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertTrue(msgs.get(0).get("body").asText().contains(
                "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n"
                        + "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\n"
                        + "A entrada é a partir das 14h"));
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/check-out", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao e json").statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        var clash = call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 2, "STANDARD"));
        assertEquals(409, clash.statusCode());
        assertEquals("ROOM_UNAVAILABLE", mapper.readTree(clash.body()).get("error").asText());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(404, call("GET", "/reservations/77", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).statusCode());
        // saída no dia da entrada de outra reserva é permitido
        create(101, "2027-03-12", "2027-03-13", 1, "STANDARD");
    }

    @Test
    void cancellationRefunds() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE");
        create(102, "2027-03-10", "2027-03-11", 1, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        assertEquals("0.00", mapper.readTree(call("POST", "/reservations/1/cancellation", null).body()).get("refund").asText());
        assertEquals("249.90", mapper.readTree(call("POST", "/reservations/2/cancellation", null).body()).get("refund").asText());
    }

    @Test
    void reports() throws Exception {
        create(101, "2027-07-10", "2027-07-13", 2, "NON_REFUNDABLE"); // 3 x 299.88 = 899.64, desc 134.95 (134.946)
        create(202, "2027-07-11", "2027-07-12", 3, "STANDARD");
        create(301, "2027-07-20", "2027-07-21", 1, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/cancellation", null); // standard: devolve tudo

        var occ = call("GET", "/reports/occupancy?date=2027-07-11", null);
        assertTrue(occ.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-13
                total,1/5
                """, occ.body());
        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-13,CONFIRMED,764.69
                total,764.69
                """, call("GET", "/reports/revenue?month=2027-07", null).body());
        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                1,101,2027-07-10,2027-07-13,2,3,27.00
                total,27.00
                """, call("GET", "/reports/tourism-tax?month=2027-07", null).body());
        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,0,0.00
                NON_REFUNDABLE,1,764.69
                total,1,764.69
                """, call("GET", "/reports/rate-plans?month=2027-07", null).body());
    }
}
