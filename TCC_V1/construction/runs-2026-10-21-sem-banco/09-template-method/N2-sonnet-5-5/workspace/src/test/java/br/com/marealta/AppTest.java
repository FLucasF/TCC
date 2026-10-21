package br.com.marealta;

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
    private Javalin app;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() { app = new App().create().start(0); }

    @AfterEach
    void stop() { app.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":%d,\"guestName\":\"Carla Dias\",\"guestEmail\":\"c@x.com\",\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"guests\":%d,\"ratePlan\":\"%s\"}"
                .formatted(room, in, out, guests, plan);
    }

    @Test
    void highSeasonPricing() throws Exception {
        var r = call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"subtotal\":599.76"), r.body());
    }

    @Test
    void confirmationSendsEmailAndReportsWork() throws Exception {
        call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        String messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"), messages);
        assertTrue(messages.contains("Total: R$ 779,80"), messages);

        String occupancy = call("GET", "/reports/occupancy?date=2027-03-10", null).body();
        assertEquals("Pousada Maré Alta,occupancy,2027-03-10\nroom,type,reservation,check_in,check_out\n"
                + "201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occupancy);
        String plans = call("GET", "/reports/rate-plans?month=2027-03", null).body();
        assertTrue(plans.endsWith("STANDARD,1,779.80\nNON_REFUNDABLE,0,0.00\ntotal,1,779.80\n"), plans);
        String tax = call("GET", "/reports/tourism-tax?month=2027-03", null).body();
        assertTrue(tax.endsWith("1,201,2027-03-10,2027-03-12,2,2,18.00\ntotal,18.00\n"), tax);
    }

    @Test
    void errorsAndRules() throws Exception {
        assertEquals(404, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(201, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 1, "NON_REFUNDABLE")).statusCode());
        assertEquals(409, call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        String cancelled = call("POST", "/reservations/1/cancellation", null).body();
        assertTrue(cancelled.contains("\"refund\":0.00"), cancelled);
        String revenue = call("GET", "/reports/revenue?month=2027-03", null).body();
        assertTrue(revenue.endsWith("1,101,2027-03-10,2027-03-12,CANCELLED,424.83\ntotal,424.83\n"), revenue);
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }
}
