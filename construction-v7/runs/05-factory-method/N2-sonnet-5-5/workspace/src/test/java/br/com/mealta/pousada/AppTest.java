package br.com.mealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class AppTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = App.create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String reservation(int room, String ident, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"c@x.com\"," + ident
                + ",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out + "\",\"guests\":" + guests
                + ",\"ratePlan\":\"" + plan + "\"}";
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void pricingHighSeasonAndDiscount() {
        Room r = new Room(101, RoomType.STANDARD);
        var q = Pricing.quote(r, java.time.LocalDate.parse("2027-07-10"), java.time.LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(Money.of("599.76"), q.total());
        var n = Pricing.quote(r, java.time.LocalDate.parse("2027-03-10"), java.time.LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("74.97"), n.discount());
    }

    @Test
    void cpfFlowWithEmailAndDocument() throws Exception {
        var res = call("POST", "/reservations", reservation(201, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, res.statusCode());
        JsonNode r = json.readTree(res.body());
        assertEquals(779.80, r.get("total").asDouble());
        assertTrue(r.get("companyCnpj").isNull());
        assertTrue(r.get("refund").isNull());
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode msgs = json.readTree(call("GET", "/messages", null).body());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertTrue(msgs.get(0).get("body").asText().contains(
                "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n"
                + "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada"));
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-out", null).statusCode());
        call("POST", "/reservations/1/check-in", null);
        JsonNode doc = json.readTree(call("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        String occ = call("GET", "/reports/occupancy?date=2027-03-10", null).body();
        assertTrue(occ.endsWith("total,1/5\n"), occ);
    }

    @Test
    void invoiceAndReceipt() throws Exception {
        call("POST", "/reservations", reservation(201, "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"", "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        String text = json.readTree(call("GET", "/reservations/1/document", null).body()).get("text").asText();
        assertTrue(text.endsWith("Total: R$ 779,80\nISS retido (5%): R$ 38,99"), text);
        call("POST", "/reservations", reservation(202, "\"guestPassport\":\"X1\",\"guestCountry\":\"United Kingdom\"", "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE"));
        call("POST", "/reservations/2/confirmation", null);
        text = json.readTree(call("GET", "/reservations/2/document", null).body()).get("text").asText();
        assertEquals("Booking receipt no. 2\nGuest: Carla Dias (passport X1, United Kingdom)\nRoom: 202 (DELUXE)\n"
                + "Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night\nNights: BRL 389.90\nDiscount: BRL 58.48\nTotal: BRL 331.42", text);
        call("POST", "/reservations/2/cancellation", null);
        String rev = call("GET", "/reports/revenue?month=2027-03", null).body();
        assertTrue(rev.contains("2,202,2027-03-10,2027-03-11,CANCELLED,331.42"), rev);
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, CPF + ",\"guestPassport\":\"X\",\"guestCountry\":\"BR\"", "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, "\"companyCnpj\":\"1\"", "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", reservation(999, CPF, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", reservation(101, CPF, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", reservation(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations", reservation(101, CPF, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }
}
