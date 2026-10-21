package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Api;
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
    private final ObjectMapper json = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = Api.create(new ReservationService()).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode book(int room, String in, String out, int guests, String plan, String company) throws Exception {
        String extra = company == null ? "" : ",\"companyCode\":\"" + company + "\"";
        var r = call("POST", "/reservations", """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"%s}""".formatted(room, in, out, guests, plan, extra));
        return json.readTree(r.body());
    }

    @Test
    void highSeasonStandard() throws Exception {
        JsonNode r = book(101, "2027-07-10", "2027-07-12", 2, "STANDARD", null);
        assertEquals(599.76, r.get("total").asDouble());
        assertEquals("599.76", r.get("total").decimalValue().toPlainString());
    }

    @Test
    void corporateWeekendLongStayFigures() throws Exception {
        JsonNode c = book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", "TECNOSUL");
        assertEquals("779.80", c.get("subtotal").decimalValue().toPlainString());
        assertEquals("93.58", c.get("discount").decimalValue().toPlainString());
        assertEquals("686.22", c.get("total").decimalValue().toPlainString());

        JsonNode w = book(202, "2027-03-12", "2027-03-14", 2, "WEEKEND_PACKAGE", null);
        assertEquals("820.00", w.get("total").decimalValue().toPlainString());
        int id = w.get("id").asInt();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode cancelled = json.readTree(call("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals("410.00", cancelled.get("refund").decimalValue().toPlainString());

        JsonNode l = book(101, "2027-03-01", "2027-03-08", 2, "LONG_STAY", null);
        assertEquals("1749.30", l.get("subtotal").decimalValue().toPlainString());
        assertEquals("174.93", l.get("discount").decimalValue().toPlainString());
        assertEquals("1574.37", l.get("total").decimalValue().toPlainString());
    }

    @Test
    void planRuleViolationsAre400() throws Exception {
        assertEquals("INVALID_REQUEST", book(101, "2027-03-10", "2027-03-12", 2, "CORPORATE", null).get("error").asText());
        assertEquals("INVALID_REQUEST", book(101, "2027-03-10", "2027-03-12", 2, "CORPORATE", "XPTO").get("error").asText());
        assertEquals("INVALID_REQUEST", book(101, "2027-03-13", "2027-03-15", 2, "WEEKEND_PACKAGE", null).get("error").asText());
        assertEquals("INVALID_REQUEST", book(101, "2027-03-01", "2027-03-07", 2, "LONG_STAY", null).get("error").asText());
    }

    @Test
    void lifecycleEmailAndReports() throws Exception {
        JsonNode r = book(201, "2027-03-10", "2027-03-12", 2, "STANDARD", null);
        int id = r.get("id").asInt();
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals("ROOM_UNAVAILABLE", book(201, "2027-03-11", "2027-03-13", 2, "STANDARD", null).get("error").asText());
        assertEquals("CAPACITY_EXCEEDED", book(101, "2027-04-11", "2027-04-13", 3, "STANDARD", null).get("error").asText());

        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode msgs = json.readTree(call("GET", "/messages", null).body());
        assertEquals(1, msgs.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertTrue(msgs.get(0).get("body").asText().contains("Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada"));

        var occ = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("content-type").orElse(""));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());

        call("POST", "/reservations/" + id + "/check-in", null);
        assertEquals("Comprovante de reserva", json.readTree(call("POST", "/reservations/" + id + "/check-out", null).body()).get("title").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }
}
