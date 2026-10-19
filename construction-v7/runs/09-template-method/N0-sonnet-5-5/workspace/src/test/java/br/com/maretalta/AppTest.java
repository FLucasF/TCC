package br.com.maretalta;

import br.com.maretalta.report.TourismTaxPolicy;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    Javalin app;
    HttpClient http = HttpClient.newHttpClient();

    @BeforeEach void start() { app = new App(TourismTaxPolicy.parse(null, null)).create().start(0); }

    @AfterEach void stop() { app.stop(); }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    String booking(int room, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":%d,\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@example.com\",\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"guests\":%d,\"ratePlan\":\"%s\"}"
                .formatted(room, in, out, guests, plan);
    }

    @Test void highSeasonPricing() throws Exception {
        var r = call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"total\":599.76"), r.body());
        assertTrue(r.body().contains("\"refund\":null"));
    }

    @Test void confirmationSendsEmailAndReceipt() throws Exception {
        call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        String msgs = call("GET", "/messages", null).body();
        assertTrue(msgs.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(msgs.contains("Diárias: R$ 779,80\\nTotal: R$ 779,80\\n\\nA entrada"), msgs);
        assertTrue(call("GET", "/reservations/1/document", null).body().contains("\"title\":\"Comprovante de reserva\""));
    }

    @Test void errorsAndConflicts() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(404, call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
    }

    @Test void reports() throws Exception {
        call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-13", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations", booking(102, "2027-07-11", "2027-07-12", 1, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/1/cancellation", null);
        call("POST", "/reservations/2/confirmation", null);
        var tax = call("GET", "/reports/tourism-tax?month=2027-07", null);
        assertEquals("Pousada Maré Alta,tourism-tax,2027-07\nreservation,room,check_in,check_out,guests,nights,tax\n2,102,2027-07-11,2027-07-12,1,1,4.50\ntotal,4.50\n", tax.body());
        var plans = call("GET", "/reports/rate-plans?month=2027-07", null);
        assertTrue(plans.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertTrue(plans.body().contains("STANDARD,1,299.88\nNON_REFUNDABLE,1,"), plans.body());
        var occ = call("GET", "/reports/occupancy?date=2027-07-11", null);
        assertTrue(occ.body().endsWith("total,1/5\n"), occ.body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).statusCode());
    }
}
