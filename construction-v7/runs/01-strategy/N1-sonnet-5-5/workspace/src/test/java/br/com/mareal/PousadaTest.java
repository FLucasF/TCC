package br.com.mareal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.mareal.api.Routes;
import br.com.mareal.domain.Outbox;
import br.com.mareal.domain.ReservationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PousadaTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        Outbox outbox = new Outbox();
        app = Routes.create(new ReservationService(outbox, Clock.systemDefaultZone()), outbox).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> book(int room, String in, String out, int guests, String plan, String company) throws Exception {
        String extra = company == null ? "" : ",\"companyCode\":\"" + company + "\"";
        return call("POST", "/reservations", """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"%s}""".formatted(room, in, out, guests, plan, extra));
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return mapper.readTree(r.body());
    }

    @Test
    void highSeasonStandard() throws Exception {
        HttpResponse<String> r = book(101, "2027-07-10", "2027-07-12", 2, "STANDARD", null);
        assertEquals(201, r.statusCode());
        assertEquals("599.76", json(r).get("total").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void corporateWeekendAndLongStayExamples() throws Exception {
        JsonNode corp = json(book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", "TECNOSUL"));
        assertEquals("779.80", corp.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("93.58", corp.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("686.22", corp.get("total").decimalValue().setScale(2).toPlainString());

        JsonNode pkg = json(book(202, "2027-03-12", "2027-03-14", 2, "WEEKEND_PACKAGE", null));
        assertEquals("820.00", pkg.get("total").decimalValue().setScale(2).toPlainString());
        call("POST", "/reservations/2/confirmation", null);
        JsonNode cancelled = json(call("POST", "/reservations/2/cancellation", null));
        assertEquals("410.00", cancelled.get("refund").decimalValue().setScale(2).toPlainString());

        JsonNode longStay = json(book(101, "2027-03-01", "2027-03-08", 1, "LONG_STAY", null));
        assertEquals("1749.30", longStay.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("174.93", longStay.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("1574.37", longStay.get("total").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void planRulesAreInvalidRequests() throws Exception {
        assertEquals(400, book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", null).statusCode());
        assertEquals(400, book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", "OUTRA").statusCode());
        assertEquals(400, book(201, "2027-03-11", "2027-03-13", 2, "WEEKEND_PACKAGE", null).statusCode());
        assertEquals(400, book(201, "2027-03-01", "2027-03-07", 2, "LONG_STAY", null).statusCode());
        assertEquals(422, book(101, "2027-03-01", "2027-03-03", 3, "STANDARD", null).statusCode());
        assertEquals(404, book(999, "2027-03-01", "2027-03-03", 1, "STANDARD", null).statusCode());
    }

    @Test
    void confirmationReceiptAndReports() throws Exception {
        for (int i = 0; i < 6; i++) {
            book(301, "2027-01-0" + (i + 1), "2027-01-0" + (i + 2), 1, "STANDARD", null);
        }
        book(201, "2027-03-10", "2027-03-12", 2, "STANDARD", null);
        assertEquals(409, book(201, "2027-03-11", "2027-03-13", 2, "STANDARD", null).statusCode());
        assertEquals(200, call("POST", "/reservations/7/confirmation", null).statusCode());
        assertEquals(409, call("POST", "/reservations/7/confirmation", null).statusCode());

        JsonNode msg = json(call("GET", "/messages", null)).get(0);
        assertEquals("EMAIL", msg.get("channel").asText());
        assertTrue(msg.get("body").asText().contains("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada"""));

        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,7,2027-03-10,2027-03-12\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2027-03-11", null).body());
        HttpResponse<String> rev = call("GET", "/reports/revenue?month=2027-03", null);
        assertEquals("reservation,room,check_in,check_out,status,amount\n7,201,2027-03-10,2027-03-12,CONFIRMED,779.80\ntotal,779.80\n", rev.body());
        assertTrue(rev.headers().firstValue("Content-Type").orElse("").toLowerCase().contains("charset=utf-8"));
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(200, call("GET", "/reservations/7/document", null).statusCode());
    }
}
