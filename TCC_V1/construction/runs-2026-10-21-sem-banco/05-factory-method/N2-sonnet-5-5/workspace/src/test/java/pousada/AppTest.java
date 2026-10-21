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
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
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
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception { return mapper.readTree(r.body()); }

    private String reserve(int room, String id, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"c@x.com\"," + id
                + ",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out + "\",\"guests\":" + guests
                + ",\"ratePlan\":\"" + plan + "\"}";
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void highSeasonPricingAndConfirmationEmail() throws Exception {
        HttpResponse<String> created = call("POST", "/reservations", reserve(101, CPF, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertEquals("599.76", json(created).get("total").asText());
        assertTrue(created.body().contains("\"refund\":null"));
        assertTrue(created.body().contains("\"companyCnpj\":null"));

        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode msgs = json(call("GET", "/messages", null));
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 101 (STANDARD)\n"
                + "Entrada: 10/07/2027 | Saída: 12/07/2027 | 2 diárias\nDiárias: R$ 599,76\nTotal: R$ 599,76\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", msgs.get(0).get("body").asText());
    }

    @Test
    void invoiceAndBookingReceipt() throws Exception {
        call("POST", "/reservations", reserve(201, "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"",
                "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        assertEquals("Fatura de hospedagem nº 1\nEmpresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)\nHóspede: Carla Dias\n"
                + "Quarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\n"
                + "Total: R$ 779,80\nISS retido (5%): R$ 38,99", json(call("GET", "/reservations/1/document", null)).get("text").asText());

        call("POST", "/reservations", reserve(202, "\"guestPassport\":\"X1234567\",\"guestCountry\":\"United Kingdom\"",
                "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE"));
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/check-in", null);
        JsonNode doc = json(call("POST", "/reservations/2/check-out", null));
        assertEquals("Booking receipt no. 2\nGuest: Carla Dias (passport X1234567, United Kingdom)\nRoom: 202 (DELUXE)\n"
                + "Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night\nNights: BRL 389.90\nDiscount: BRL 58.48\n"
                + "Total: BRL 331.42", doc.get("text").asText());
        assertTrue(doc.get("guestCpf").isNull());
    }

    @Test
    void errorsAndCancellation() throws Exception {
        assertEquals(400, call("POST", "/reservations", reserve(101, CPF + ",\"companyName\":\"X\"", "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reserve(101, "\"guestCpf\":\"1\",\"guestPassport\":\"2\"", "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(404, call("POST", "/reservations", reserve(999, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", reserve(101, CPF, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        call("POST", "/reservations", reserve(101, CPF, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        assertEquals(409, call("POST", "/reservations", reserve(101, CPF, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        HttpResponse<String> cancelled = call("POST", "/reservations/1/cancellation", null);
        assertTrue(cancelled.body().contains("\"refund\":0.00"));
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(404, call("GET", "/reservations/77", null).statusCode());
        assertEquals(201, call("POST", "/reservations", reserve(101, CPF, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,101,2027-03-10,2027-03-12,CANCELLED,424.83\ntotal,424.83\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027", null).statusCode());
    }

    @Test
    void occupancyReport() throws Exception {
        call("POST", "/reservations", reserve(301, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        assertEquals("room,type,reservation,check_in,check_out\n301,SUITE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2027-03-11", null).body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());
    }
}
