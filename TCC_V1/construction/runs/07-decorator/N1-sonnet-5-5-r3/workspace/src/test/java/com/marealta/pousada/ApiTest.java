package com.marealta.pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marealta.pousada.domain.*;
import com.marealta.pousada.mail.MailRules;
import com.marealta.pousada.mail.MailService;
import com.marealta.pousada.web.Api;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    private void start(boolean testMode) {
        var reservationRepo = new InMemory.Reservations();
        var rooms = new RoomCatalog();
        var mail = new MailService(new InMemory.Messages(), MailRules.marealta(), testMode);
        app = new Api(new ReservationService(reservationRepo, rooms, mail), new ReportService(reservationRepo, rooms),
                mail, rooms).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        if (app != null) app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String booking(int room, String email, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    private JsonNode body(HttpResponse<String> r) throws Exception {
        return json.readerFor(JsonNode.class).with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(r.body());
    }

    @Test
    void fullFlowWithReceiptAndArchiveCopy() throws Exception {
        start(false);
        var created = call("POST", "/reservations", booking(201, "carla@x.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertEquals("PENDING", body(created).get("status").asText());
        assertEquals(779.80, body(created).get("total").asDouble());
        assertTrue(body(created).get("refund").isNull());

        var confirmed = call("POST", "/reservations/1/confirmation", null);
        assertTrue(confirmed.body().contains("\"amountPaid\":779.80"), confirmed.body());

        var messages = body(call("GET", "/messages", null));
        assertEquals(2, messages.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).get("subject").asText());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).get("to").asText());
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
                Até breve!""", messages.get(0).get("body").asText());

        assertEquals(409, call("POST", "/reservations/1/check-out", null).statusCode());
        assertEquals("CHECKED_IN", body(call("POST", "/reservations/1/check-in", null)).get("status").asText());
        var doc = call("POST", "/reservations/1/check-out", null);
        assertEquals("Comprovante de reserva", body(doc).get("title").asText());
        assertEquals("DELUXE", body(doc).get("roomType").asText());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());

        var occupancy = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occupancy.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void invalidAddressIsBlockedWithoutCopy() throws Exception {
        start(false);
        call("POST", "/reservations", booking(101, "ana@gmail", "2027-03-10", "2027-03-11", 1, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        var messages = body(call("GET", "/messages", null));
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
        assertEquals("ana@gmail", messages.get(0).get("to").asText());
    }

    @Test
    void testModeRedirectsAndSkipsArchive() throws Exception {
        start(true);
        call("POST", "/reservations", booking(101, "ana@gmail.com", "2027-03-10", "2027-03-11", 1, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations", booking(102, "ana.gmail.com", "2027-03-10", "2027-03-11", 1, "STANDARD"));
        call("POST", "/reservations/2/confirmation", null);
        var messages = body(call("GET", "/messages", null));
        assertEquals(2, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).get("to").asText());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertTrue(messages.get(0).get("subject").asText().startsWith("[TESTE] Reserva"));
        assertTrue(messages.get(0).get("body").asText().startsWith("Destinatário original: ana@gmail.com\nOlá, Carla"));
        assertEquals("ana.gmail.com", messages.get(1).get("to").asText());
        assertEquals("BLOCKED", messages.get(1).get("status").asText());
        assertFalse(messages.get(1).get("subject").asText().startsWith("["));
    }

    @Test
    void cancellationRefundsAndConflicts() throws Exception {
        start(false);
        call("POST", "/reservations", booking(101, "a@b.com", "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE"));
        assertEquals(409, call("POST", "/reservations", booking(101, "a@b.com", "2027-07-11", "2027-07-13", 2, "STANDARD")).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        var cancelledResponse = call("POST", "/reservations/1/cancellation", null);
        assertTrue(cancelledResponse.body().contains("\"amountPaid\":509.80,"), cancelledResponse.body());
        assertTrue(cancelledResponse.body().contains("\"refund\":0.00"), cancelledResponse.body());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(201, call("POST", "/reservations", booking(101, "a@b.com", "2027-07-11", "2027-07-13", 2, "STANDARD")).statusCode());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,101,2027-07-10,2027-07-12,CANCELLED,509.80\ntotal,509.80\n"
                , call("GET", "/reports/revenue?month=2027-07", null).body());
    }

    @Test
    void errors() throws Exception {
        start(false);
        assertEquals(400, call("POST", "/reservations", "nao json").statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "a@b.com", "2027-07-12", "2027-07-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "a@b.com", "2027-07-10", "2027-07-12", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", booking(999, "a@b.com", "2027-07-10", "2027-07-12", 1, "STANDARD")).statusCode());
        var capacity = call("POST", "/reservations", booking(101, "a@b.com", "2027-07-10", "2027-07-12", 3, "STANDARD"));
        assertEquals(422, capacity.statusCode());
        assertEquals("CAPACITY_EXCEEDED", body(capacity).get("error").asText());
        assertEquals("RESERVATION_NOT_FOUND", body(call("GET", "/reservations/77", null)).get("error").asText());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy", null).statusCode());
        assertEquals(5, body(call("GET", "/rooms", null)).size());
    }
}
