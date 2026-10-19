package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InnTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = App.create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return MAPPER.readTree(r.body());
    }

    private String reservation(int room, String ident, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com",%s,
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(
                room, ident, in, out, guests, plan);
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void highSeasonPricing() throws Exception {
        HttpResponse<String> r = call("POST", "/reservations",
                reservation(101, CPF, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"total\":599.76"));
        assertTrue(json(r).get("guestCnpj") == null);
        assertTrue(json(r).get("companyCnpj").isNull());
    }

    @Test
    void confirmationSendsEmailWithReceipt() throws Exception {
        call("POST", "/reservations", reservation(201, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode m = json(call("GET", "/messages", null)).get(0);
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.get("subject").asText());
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
                Até breve!""", m.get("body").asText());
    }

    @Test
    void invoiceAndBookingReceipt() throws Exception {
        call("POST", "/reservations", reservation(201,
                "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"",
                "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations", reservation(202,
                "\"guestPassport\":\"X1234567\",\"guestCountry\":\"United Kingdom\"",
                "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        JsonNode invoice = json(call("GET", "/reservations/1/document", null));
        assertEquals("Fatura de hospedagem", invoice.get("title").asText());
        assertTrue(invoice.get("text").asText().endsWith("Total: R$ 779,80\nISS retido (5%): R$ 38,99"));
        assertTrue(invoice.get("guestCpf").isNull());
        JsonNode receipt = json(call("GET", "/reservations/2/document", null));
        assertEquals("""
                Booking receipt no. 2
                Guest: Carla Dias (passport X1234567, United Kingdom)
                Room: 202 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42""", receipt.get("text").asText());
    }

    @Test
    void identificationMustBeExactlyOneKind() throws Exception {
        for (String ident : new String[] {
                "\"guestName2\":\"x\"",
                CPF + ",\"guestPassport\":\"A1\",\"guestCountry\":\"BR\"",
                "\"companyCnpj\":\"1\"",
                "\"guestPassport\":\"A1\""}) {
            HttpResponse<String> r = call("POST", "/reservations",
                    reservation(101, ident, "2027-03-10", "2027-03-12", 2, "STANDARD"));
            assertEquals(400, r.statusCode(), ident);
            assertEquals("INVALID_REQUEST", json(r).get("error").asText());
        }
    }

    @Test
    void rulesAndErrors() throws Exception {
        assertEquals(422, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations",
                reservation(999, CPF, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "nao e json").statusCode());
        call("POST", "/reservations", reservation(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(409, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-12", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
    }

    @Test
    void cancellationRefundAndReports() throws Exception {
        call("POST", "/reservations", reservation(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations", reservation(102, CPF, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        String occupancy = call("GET", "/reports/occupancy?date=2027-03-11", null).body();
        assertEquals("room,type,reservation,check_in,check_out\n"
                + "101,STANDARD,1,2027-03-10,2027-03-12\n102,STANDARD,2,2027-03-10,2027-03-12\ntotal,2/5\n", occupancy);
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));
        assertTrue(call("POST", "/reservations/1/cancellation", null).body().contains("\"refund\":499.80"));
        assertTrue(call("POST", "/reservations/2/cancellation", null).body().contains("\"refund\":0.00"));
        HttpResponse<String> revenue = call("GET", "/reports/revenue?month=2027-03", null);
        assertTrue(revenue.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + "2,102,2027-03-10,2027-03-12,CANCELLED,424.83\ntotal,424.83\n", revenue.body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }

    @Test
    void checkOutReturnsDocument() throws Exception {
        call("POST", "/reservations", reservation(301, CPF, "2027-03-10", "2027-03-11", 4, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/1/check-in", null);
        JsonNode doc = json(call("POST", "/reservations/1/check-out", null));
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals(1, doc.get("nights").asInt());
        assertEquals(200, call("GET", "/rooms", null).statusCode());
    }
}
