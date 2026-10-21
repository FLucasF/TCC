package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.*;

import br.com.marealta.pousada.domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.*;

class SystemTest {
    static final ObjectMapper M = new ObjectMapper();
    Javalin app;
    HttpClient http = HttpClient.newHttpClient();
    String base;

    @BeforeEach
    void start() {
        app = Main.build().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    JsonNode json(HttpResponse<String> r) throws Exception {
        return M.readTree(r.body());
    }

    static String req(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void pricingExamples() {
        Room std = ReservationService.defaultRooms().get(0);
        var q = Pricing.quote(std, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(new java.math.BigDecimal("599.76"), q.total());
        assertEquals("R$ 1.234,56", Money.format(new java.math.BigDecimal("1234.56")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-03-01")));
    }

    @Test
    void fullFlowWithNotifications() throws Exception {
        var created = call("POST", "/reservations", req(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertEquals(779.80, json(created).get("total").asDouble());
        assertEquals(0, json(created).get("amountPaid").asDouble());
        assertTrue(json(created).get("refund").isNull());
        assertEquals("[]", call("GET", "/messages", null).body());

        var conf = call("POST", "/reservations/1/confirmation", null);
        assertEquals("CONFIRMED", json(conf).get("status").asText());
        JsonNode msgs = json(call("GET", "/messages", null));
        assertEquals(3, msgs.size());
        String all = msgs.toString();
        assertTrue(all.contains("Reserva confirmada - quarto 201"));
        assertTrue(all.contains("Pagamento recebido - reserva 1"));
        assertTrue(all.contains("R$ 779,80"));
        JsonNode guest = msgs.get(0);
        assertEquals("carla@x.com", guest.get("to").asText());
        assertEquals("EMAIL", guest.get("channel").asText());
        assertTrue(guest.get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva"));

        var cancel = call("POST", "/reservations/1/cancellation", null);
        assertEquals(779.80, json(cancel).get("refund").asDouble());
        assertEquals(5, json(call("GET", "/messages", null)).size());
        assertTrue(call("GET", "/messages", null).body().contains("Reembolso - reserva 1"));
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void nonRefundableCancelAndPendingCancel() throws Exception {
        call("POST", "/reservations", req(101, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        int before = json(call("GET", "/messages", null)).size();
        var c = json(call("POST", "/reservations/1/cancellation", null));
        assertEquals(0, c.get("refund").asDouble());
        assertEquals(before + 1, json(call("GET", "/messages", null)).size()); // governança only

        call("POST", "/reservations", req(101, "2027-03-10", "2027-03-11", 1, "STANDARD"));
        int n = json(call("GET", "/messages", null)).size();
        call("POST", "/reservations/2/cancellation", null);
        assertEquals(n, json(call("GET", "/messages", null)).size());
    }

    @Test
    void errorsAndAvailability() throws Exception {
        assertEquals(201, call("POST", "/reservations", req(101, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals("ROOM_UNAVAILABLE", json(call("POST", "/reservations", req(101, "2027-03-11", "2027-03-13", 2, "STANDARD"))).get("error").asText());
        assertEquals(201, call("POST", "/reservations", req(101, "2027-03-12", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", req(102, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", req(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", req(101, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", req(101, "2027-03-10", "2027-03-12", 0, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "not json").statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy", null).statusCode());
    }

    @Test
    void checkoutAndReports() throws Exception {
        call("POST", "/reservations", req(301, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE"));
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        assertEquals(409, call("POST", "/reservations/1/check-out", null).statusCode());
        call("POST", "/reservations/1/check-in", null);
        var doc = call("GET", "/reservations/1/document", null);
        assertEquals("Comprovante de reserva", json(doc).get("title").asText());
        var out = json(call("POST", "/reservations/1/check-out", null));
        assertEquals("SUITE", out.get("roomType").asText());
        assertEquals(1391.76, out.get("subtotal").asDouble());
        assertEquals(208.76, out.get("discount").asDouble());

        var occ = call("GET", "/reports/occupancy?date=2027-07-11", null);
        assertTrue(occ.headers().firstValue("Content-Type").get().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n301,SUITE,1,2027-07-10,2027-07-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,301,2027-07-10,2027-07-12,CHECKED_OUT,1183.00\ntotal,1183.00\n",
                call("GET", "/reports/revenue?month=2027-07", null).body());
    }

    @Test
    void roomsEndpoint() throws Exception {
        JsonNode rooms = json(call("GET", "/rooms", null));
        assertEquals(5, rooms.size());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
    }
}
