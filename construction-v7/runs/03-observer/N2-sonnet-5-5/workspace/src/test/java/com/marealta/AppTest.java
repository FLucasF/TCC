package com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppTest {
    private Javalin server;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void up() { server = new App().start(0); }

    @AfterEach
    void down() { server.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String body(int room, String in, String out, int guests, String plan) {
        return """
            {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
             "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void highSeasonPricing() {
        assertEquals("299.88", Pricing.nightPrice(new Room(101, RoomType.STANDARD), LocalDate.parse("2027-07-10")).toPlainString());
        assertEquals("599.76", Pricing.subtotal(new Room(101, RoomType.STANDARD),
                LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12")).toPlainString());
    }

    @Test
    void confirmationNotifiesEveryone() throws Exception {
        var created = call("POST", "/reservations", body(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":779.80"));
        call("POST", "/reservations/1/confirmation", null);
        String messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(messages.contains("Reserva confirmada - quarto 201"));
        assertTrue(messages.contains("Pagamento recebido - reserva 1"));
        assertTrue(messages.contains("Comprovante de reserva nº 1\\nHóspede: Carla Dias (CPF 111.222.333-44)"));
    }

    @Test
    void cancelRules() throws Exception {
        call("POST", "/reservations", body(201, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/cancellation", null);
        assertEquals("[]", call("GET", "/messages", null).body());
        call("POST", "/reservations", body(201, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/2/confirmation", null);
        var cancelled = call("POST", "/reservations/2/cancellation", null);
        assertTrue(cancelled.body().contains("\"refund\":0.00"));
        String messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva cancelada - quarto 201"));
        assertFalse(messages.contains("Reembolso"));
        assertEquals(409, call("POST", "/reservations/2/check-in", null).statusCode());
    }

    @Test
    void errorsAndReports() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(404, call("POST", "/reservations", body(999, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", body(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE"));
        assertEquals(409, call("POST", "/reservations", body(101, "2027-07-11", "2027-07-13", 2, "STANDARD")).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD,1,2027-07-10,2027-07-12\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2027-07-11", null).body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,101,2027-07-10,2027-07-12,CONFIRMED,509.80\ntotal,509.80\n",
                call("GET", "/reports/revenue?month=2027-07", null).body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).statusCode());
    }
}
