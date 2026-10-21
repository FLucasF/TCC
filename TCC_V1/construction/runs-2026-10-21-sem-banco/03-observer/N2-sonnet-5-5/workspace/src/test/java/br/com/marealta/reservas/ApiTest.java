package br.com.marealta.reservas;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() {
        app = Main.build(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    static HttpResponse<String> call(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String body(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void highSeasonPricingAndFullFlow() throws Exception {
        var created = call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":599.76"), created.body());
        assertTrue(created.body().contains("\"refund\":null"));
        int before = call("GET", "/messages", null).body().split("\"id\"").length;

        var conflict = call("POST", "/reservations", body(101, "2027-07-11", "2027-07-13", 1, "STANDARD"));
        assertEquals(409, conflict.statusCode());
        assertTrue(conflict.body().contains("ROOM_UNAVAILABLE"));

        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        String messages = call("GET", "/messages", null).body();
        assertEquals(before + 3, messages.split("\"id\"").length);
        assertTrue(messages.contains("Pagamento recebido - reserva 1"));
        assertTrue(messages.contains("Reserva confirmada - quarto 101"));
        assertTrue(messages.contains("R$ 599,76"));

        String csv = call("GET", "/reports/occupancy?date=2027-07-10", null).body();
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD,1,2027-07-10,2027-07-12\ntotal,1/5\n", csv);
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        var out = call("POST", "/reservations/1/check-out", null);
        assertTrue(out.body().contains("\"title\":\"Comprovante de reserva\""));
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76\ntotal,599.76\n",
                call("GET", "/reports/revenue?month=2027-07", null).body());
    }

    @Test
    void cancellationRulesAndNotifications() throws Exception {
        // non-refundable confirmed: 2 nights of 249.90 = 499.80, 15% = 74.97 -> 424.83, no refund, finance not told
        call("POST", "/reservations", body(102, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations", body(202, "2027-03-10", "2027-03-11", 3, "STANDARD"));
        call("POST", "/reservations", body(201, "2027-03-10", "2027-03-11", 3, "STANDARD"));
        call("POST", "/reservations", body(301, "2027-03-10", "2027-03-11", 3, "STANDARD"));
        int base0 = call("GET", "/messages", null).body().split("\"id\"").length;
        // pending cancelled: nobody told
        var pendingCancel = call("POST", "/reservations/4/cancellation", null);
        assertEquals(200, pendingCancel.statusCode());
        assertTrue(pendingCancel.body().contains("\"refund\":0.00"));
        assertEquals(base0, call("GET", "/messages", null).body().split("\"id\"").length);
        call("POST", "/reservations/1/confirmation", null);
        var cancelled = call("POST", "/reservations/1/cancellation", null);
        assertTrue(cancelled.body().contains("\"amountPaid\":424.83"), cancelled.body());
        assertTrue(cancelled.body().contains("\"refund\":0.00"));
        String messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva cancelada - quarto 102"));
        assertFalse(messages.contains("Reembolso - reserva 1"));
        // refundable confirmed -> finance told
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/cancellation", null);
        assertTrue(call("GET", "/messages", null).body().contains("Reembolso - reserva 2"));
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "not json").statusCode());
        assertEquals(400, call("POST", "/reservations", body(101, "2027-05-10", "2027-05-10", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", body(101, "2027-05-10", "2027-05-11", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", body(999, "2027-05-10", "2027-05-11", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", body(101, "2027-05-10", "2027-05-11", 3, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/9999", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy", null).statusCode());
        assertTrue(call("GET", "/rooms", null).body().contains("\"nightlyRate\":249.90"));
    }
}
