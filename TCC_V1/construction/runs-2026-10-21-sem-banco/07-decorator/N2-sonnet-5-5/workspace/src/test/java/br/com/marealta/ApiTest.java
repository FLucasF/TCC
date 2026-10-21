package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {
    private Javalin app;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void start() { app = Main.createApp(false).start(0); }

    @AfterEach
    void stop() { app.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":%d,\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@x.com\",\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"guests\":%d,\"ratePlan\":\"%s\"}"
                .formatted(room, in, out, guests, plan);
    }

    @Test
    void highSeasonPricing() throws Exception {
        var r = call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"total\":599.76"), r.body());
        assertTrue(r.body().contains("\"refund\":null"), r.body());
    }

    @Test
    void fullFlowAndReceipt() throws Exception {
        call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        var c = call("POST", "/reservations/1/confirmation", null);
        assertTrue(c.body().contains("\"amountPaid\":779.80"), c.body());
        var msgs = call("GET", "/messages", null).body();
        assertTrue(msgs.contains("Reserva confirmada - Pousada Maré Alta"), msgs);
        assertTrue(msgs.contains("Diárias: R$ 779,80"), msgs);
        assertTrue(msgs.contains("[Cópia] "), msgs);
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-out", null).statusCode());
        call("POST", "/reservations/1/check-in", null);
        assertEquals("2/5".length(), 3);
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-11", null).body().endsWith("total,1/5\n"));
        assertEquals(200, call("POST", "/reservations/1/check-out", null).statusCode());
        assertTrue(call("GET", "/reports/revenue?month=2027-03", null).body().endsWith("total,779.80\n"));
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao e json").statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", booking(999, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(409, call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/77", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }

    @Test
    void nonRefundableCancellationKeepsMoney() throws Exception {
        call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        var c = call("POST", "/reservations/1/cancellation", null);
        assertTrue(c.body().contains("\"refund\":0.00"), c.body());
        assertTrue(call("GET", "/reports/revenue?month=2027-03", null).body().contains("CANCELLED,424.83"));
    }
}
