package pousada;

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
import pousada.web.Api;

class ApiTest {
    private Javalin app;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() {
        app = Api.create().start(0);
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
                .method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String body(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void highSeasonPricingAndConfirmationEmail() throws Exception {
        var created = call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":599.76"), created.body());
        assertTrue(created.body().contains("\"refund\":null"));

        call("POST", "/reservations/1/confirmation", null);
        var messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"), messages);
        assertTrue(messages.contains("Diárias: R$ 599,76\\nTotal: R$ 599,76"), messages);
    }

    @Test
    void nonRefundableDiscountAndCancellationKeepsMoney() throws Exception {
        call("POST", "/reservations", body(201, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        var cancelled = call("POST", "/reservations/1/cancellation", null);
        assertTrue(cancelled.body().contains("\"discount\":116.97"), cancelled.body());
        assertTrue(cancelled.body().contains("\"refund\":0.00"), cancelled.body());

        var revenue = call("GET", "/reports/revenue?month=2027-03", null);
        assertEquals("Pousada Maré Alta,revenue,2027-03\n"
                + "reservation,room,check_in,check_out,status,amount\n"
                + "1,201,2027-03-10,2027-03-12,CANCELLED,662.83\n"
                + "total,662.83\n", revenue.body());
        assertTrue(revenue.headers().firstValue("content-type").orElse("").contains("text/csv"));
    }

    @Test
    void errors() throws Exception {
        assertEquals(404, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(400, call("POST", "/reservations", "nao e json").statusCode());
        assertEquals(400, call("POST", "/reservations", body(101, "2027-07-12", "2027-07-12", 1, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", body(999, "2027-07-10", "2027-07-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 3, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 2, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations", body(101, "2027-07-11", "2027-07-13", 2, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(404, call("GET", "/reservations/77", null).statusCode());
        assertEquals(400, call("GET", "/reports/rate-plans?month=2027-7", null).statusCode());
    }

    @Test
    void occupancyTaxAndPlansReports() throws Exception {
        call("POST", "/reservations", body(202, "2027-07-10", "2027-07-13", 2, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/1/check-in", null);
        var out = call("POST", "/reservations/1/check-out", null);
        assertTrue(out.body().contains("\"title\":\"Comprovante de reserva\""), out.body());

        assertEquals("Pousada Maré Alta,occupancy,2027-07-12\nroom,type,reservation,check_in,check_out\n"
                + "202,DELUXE,1,2027-07-10,2027-07-13\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2027-07-12", null).body());
        assertEquals("Pousada Maré Alta,tourism-tax,2027-07\nreservation,room,check_in,check_out,guests,nights,tax\n"
                + "1,202,2027-07-10,2027-07-13,2,3,27.00\ntotal,27.00\n",
                call("GET", "/reports/tourism-tax?month=2027-07", null).body());
        assertEquals("Pousada Maré Alta,rate-plans,2027-08\nrate_plan,reservations,amount\n"
                + "STANDARD,0,0.00\nNON_REFUNDABLE,0,0.00\ntotal,0,0.00\n",
                call("GET", "/reports/rate-plans?month=2027-08", null).body());
    }
}
