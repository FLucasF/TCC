package br.com.marealta;

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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        Outbox outbox = new Outbox();
        app = new Api(new ReservationService(outbox), outbox).create().start(0);
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
        return om.readTree(r.body());
    }

    private static String cpfBody(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void pricingExamples() {
        Room r101 = Room.ALL.get(0);
        var q = Pricing.quote(r101, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals("599.76", q.total().toPlainString());
        var n = Pricing.quote(r101, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.NON_REFUNDABLE);
        assertEquals("89.96", n.discount().toPlainString());
        assertEquals("509.80", n.total().toPlainString());
        assertTrue(Pricing.highSeason(LocalDate.parse("2027-12-15")));
        assertFalse(Pricing.highSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Pricing.highSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Pricing.highSeason(LocalDate.parse("2027-03-01")));
    }

    @Test
    void confirmationEmailAndDocument() throws Exception {
        var created = call("POST", "/reservations", cpfBody(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertTrue(created.body().contains("\"refund\":null"));
        call("POST", "/reservations/1/confirmation", null);
        JsonNode msgs = json(call("GET", "/messages", null));
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
        assertEquals(409, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void invoiceAndBookingReceipt() throws Exception {
        call("POST", "/reservations", """
                {"roomNumber":201,"guestName":"Ana Souza","guestEmail":"a@x.com","companyCnpj":"12.345.678/0001-90",
                 "companyName":"Tecnosul Ltda","checkIn":"2027-03-10","checkOut":"2027-03-12","guests":1,"ratePlan":"STANDARD"}""");
        call("POST", "/reservations/1/confirmation", null);
        assertEquals("""
                Fatura de hospedagem nº 1
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", json(call("GET", "/reservations/1/document", null)).get("text").asText());

        call("POST", "/reservations", """
                {"roomNumber":202,"guestName":"John Smith","guestEmail":"j@x.com","guestPassport":"X1234567",
                 "guestCountry":"United Kingdom","checkIn":"2027-03-10","checkOut":"2027-03-12","guests":1,"ratePlan":"STANDARD"}""");
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/check-in", null);
        JsonNode out = json(call("POST", "/reservations/2/check-out", null));
        assertEquals("Booking receipt", out.get("title").asText());
        assertTrue(out.get("guestCpf").isNull());
        assertEquals("""
                Booking receipt no. 2
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 202 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80""", out.get("text").asText());
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(400, call("POST", "/reservations", cpfBody(101, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", cpfBody(101, "2027-03-10", "2027-03-12", 0, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", cpfBody(101, "2027-3-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", cpfBody(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", cpfBody(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        String both = cpfBody(101, "2027-03-10", "2027-03-12", 1, "STANDARD").replace("\"guestCpf\"", "\"guestPassport\":\"A\",\"guestCountry\":\"B\",\"guestCpf\"");
        assertEquals(400, call("POST", "/reservations", both).statusCode());
        assertEquals(201, call("POST", "/reservations", cpfBody(101, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations", cpfBody(101, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", cpfBody(101, "2027-03-12", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }

    @Test
    void cancellationAndReports() throws Exception {
        call("POST", "/reservations", cpfBody(101, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        var cancelled = call("POST", "/reservations/1/cancellation", null).body();
        assertTrue(cancelled.contains("\"refund\":0.00"), cancelled);
        assertTrue(cancelled.contains("\"amountPaid\":509.80"), cancelled);
        call("POST", "/reservations", cpfBody(102, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        call("POST", "/reservations/2/confirmation", null);
        var rev = call("GET", "/reports/revenue?month=2027-07", null);
        assertTrue(rev.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CANCELLED,509.80
                2,102,2027-07-10,2027-07-12,CONFIRMED,599.76
                total,1109.56
                """, rev.body());
        assertEquals("""
                room,type,reservation,check_in,check_out
                102,STANDARD,2,2027-07-10,2027-07-12
                total,1/5
                """, call("GET", "/reports/occupancy?date=2027-07-11", null).body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());
    }
}
