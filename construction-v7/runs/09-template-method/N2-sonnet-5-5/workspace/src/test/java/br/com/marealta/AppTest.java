package br.com.marealta;

import br.com.marealta.web.App;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    static Javalin app;
    static HttpClient http = HttpClient.newHttpClient();

    @BeforeAll static void start() { app = App.create().start(0); }

    @AfterAll static void stop() { app.stop(); }

    static HttpResponse<String> call(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"11122233344",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void fullFlowAndReports() throws Exception {
        var created = call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":599.76"), created.body());

        assertEquals(409, call("POST", "/reservations", booking(101, "2027-07-11", "2027-07-13", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", booking(101, "2027-08-11", "2027-08-13", 3, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());

        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        var messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"), messages);

        var occupancy = call("GET", "/reports/occupancy?date=2027-07-10", null);
        assertEquals("Pousada Maré Alta,occupancy,2027-07-10\n"
                + "room,type,reservation,check_in,check_out\n"
                + "101,STANDARD,1,2027-07-10,2027-07-12\n"
                + "total,1/5\n", occupancy.body());
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));

        assertEquals("Pousada Maré Alta,tourism-tax,2027-07\n"
                + "reservation,room,check_in,check_out,guests,nights,tax\n"
                + "1,101,2027-07-10,2027-07-12,2,2,18.00\n"
                + "total,18.00\n", call("GET", "/reports/tourism-tax?month=2027-07", null).body());

        assertEquals("Pousada Maré Alta,rate-plans,2027-07\n"
                + "rate_plan,reservations,amount\n"
                + "STANDARD,1,599.76\n"
                + "NON_REFUNDABLE,0,0.00\n"
                + "total,1,599.76\n", call("GET", "/reports/rate-plans?month=2027-07", null).body());

        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        var out = call("POST", "/reservations/1/check-out", null);
        assertTrue(out.body().contains("\"title\":\"Comprovante de reserva\""), out.body());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
    }
}
