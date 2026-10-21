package br.com.pousadamarealta;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AppTest {
    private static final ObjectMapper M = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    private void start(AppConfig cfg, Clock clock) {
        app = App.create(cfg, clock).start(0);
        base = "http://localhost:" + app.port();
    }

    private void start(AppConfig cfg) {
        start(cfg, Clock.systemUTC());
    }

    @AfterEach
    void stop() {
        if (app != null) app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode book(int room, String email, String in, String out, int guests, String plan) throws Exception {
        var r = call("POST", "/reservations", """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return M.readTree(r.body());
    }

    @Test
    void highSeasonPricingAndFlow() throws Exception {
        start(AppConfig.defaults());
        JsonNode r = book(101, "a@b.com", "2027-07-10", "2027-07-12", 2, "STANDARD");
        assertEquals(1, r.get("id").asInt());
        assertEquals("599.76", r.get("total").decimalValue().toPlainString());
        assertTrue(call("GET", "/reservations/1", null).body().contains("\"amountPaid\":0.00"));
        assertTrue(r.get("refund").isNull());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/check-out", null).statusCode());
    }

    @Test
    void errors() throws Exception {
        start(AppConfig.defaults());
        book(201, "a@b.com", "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE");
        assertEquals(409, call("POST", "/reservations", """
                {"roomNumber":201,"guestName":"x","guestEmail":"a@b.com","guestCpf":"1","checkIn":"2027-03-11",
                 "checkOut":"2027-03-13","guests":1,"ratePlan":"STANDARD"}""").statusCode());
        assertEquals(422, call("POST", "/reservations", """
                {"roomNumber":101,"guestName":"x","guestEmail":"a@b.com","guestCpf":"1","checkIn":"2027-03-11",
                 "checkOut":"2027-03-13","guests":3,"ratePlan":"STANDARD"}""").statusCode());
        assertEquals(404, call("POST", "/reservations", """
                {"roomNumber":999,"guestName":"x","guestEmail":"a@b.com","guestCpf":"1","checkIn":"2027-03-11",
                 "checkOut":"2027-03-13","guests":1,"ratePlan":"STANDARD"}""").statusCode());
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(404, call("GET", "/reservations/77", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }

    @Test
    void cancellationRefunds() throws Exception {
        start(AppConfig.defaults());
        book(101, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        book(102, "a@b.com", "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE");
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        assertTrue(call("POST", "/reservations/1/cancellation", null).body().contains("\"refund\":499.80"));
        assertTrue(call("POST", "/reservations/2/cancellation", null).body().contains("\"refund\":0.00"));
        String rev = call("GET", "/reports/revenue?month=2027-03", null).body();
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                2,102,2027-03-10,2027-03-12,CANCELLED,424.83
                total,424.83
                """, rev);
    }

    @Test
    void liveModeSendsConfirmationAndArchiveCopy() throws Exception {
        start(AppConfig.defaults());
        book(201, "carla@x.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        JsonNode msgs = M.readTree(call("GET", "/messages", null).body());
        assertEquals(2, msgs.size());
        assertEquals("carla@x.com", msgs.get(0).get("to").asText());
        assertEquals("SENT", msgs.get(0).get("status").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertTrue(msgs.get(0).get("body").asText().contains("Total: R$ 779,80\n\nA entrada"));
        assertEquals("arquivo@pousadamarealta.com.br", msgs.get(1).get("to").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", msgs.get(1).get("subject").asText());
        assertEquals(msgs.get(0).get("body"), msgs.get(1).get("body"));
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() throws Exception {
        start(AppConfig.defaults());
        book(201, "ana@gmail", "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        JsonNode msgs = M.readTree(call("GET", "/messages", null).body());
        assertEquals(1, msgs.size());
        assertEquals("BLOCKED", msgs.get(0).get("status").asText());
        assertEquals("ana@gmail", msgs.get(0).get("to").asText());
    }

    @Test
    void testModeRedirects() throws Exception {
        start(AppConfig.defaults().withTestMode(true));
        book(201, "carla@x.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        book(202, "ana.gmail.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        JsonNode msgs = M.readTree(call("GET", "/messages", null).body());
        assertEquals(2, msgs.size());
        assertEquals("testes@pousadamarealta.com.br", msgs.get(0).get("to").asText());
        assertTrue(msgs.get(0).get("subject").asText().startsWith("[TESTE] "));
        assertTrue(msgs.get(0).get("body").asText().startsWith("Destinatário original: carla@x.com\nOlá, Carla"));
        assertEquals("BLOCKED", msgs.get(1).get("status").asText());
        assertEquals("ana.gmail.com", msgs.get(1).get("to").asText());
    }

    @Test
    void quietHoursAndDailyLimitAreConfigurable() throws Exception {
        Clock night = Clock.fixed(Instant.parse("2027-03-01T03:00:00Z"), ZoneId.of("UTC"));
        start(AppConfig.defaults().withQuietHours(Scope.LIVE_ONLY, 0, 6)
                .withDailyLimit(Scope.ALWAYS, 1).withArchiveCopy(Scope.OFF)
                .withTestMode(false), night);
        // 03:00 UTC is 00:00 in São Paulo, inside the quiet window
        book(201, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        JsonNode msgs = M.readTree(call("GET", "/messages", null).body());
        assertEquals("BLOCKED", msgs.get(0).get("status").asText());
        app.stop();

        Clock day = Clock.fixed(Instant.parse("2027-03-01T15:00:00Z"), ZoneId.of("UTC"));
        start(AppConfig.defaults().withDailyLimit(Scope.ALWAYS, 1).withArchiveCopy(Scope.OFF), day);
        book(201, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        book(202, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        msgs = M.readTree(call("GET", "/messages", null).body());
        assertEquals("SENT", msgs.get(0).get("status").asText());
        assertEquals("BLOCKED", msgs.get(1).get("status").asText());
    }

    @Test
    void occupancyReport() throws Exception {
        start(AppConfig.defaults());
        book(301, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        assertEquals("""
                room,type,reservation,check_in,check_out
                301,SUITE,1,2027-03-10,2027-03-12
                total,1/5
                """, call("GET", "/reports/occupancy?date=2027-03-11", null).body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());
    }
}
