package br.com.marealta;

import br.com.marealta.web.App;
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
    private Javalin app;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void start() { app = App.create(false).start(0); }

    @AfterEach
    void stop() { app.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String body(String room, String in, String out, int guests, String plan) {
        return """
            {"roomNumber":%s,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
             "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        var r = call("POST", "/reservations", body("201", "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"total\":779.80"), r.body());
        assertTrue(r.body().contains("\"refund\":null"));

        assertEquals(409, call("POST", "/reservations", body("201", "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());

        r = call("POST", "/reservations/1/confirmation", null);
        assertTrue(r.body().contains("\"amountPaid\":779.80"), r.body());
        var msgs = call("GET", "/messages", null).body();
        assertTrue(msgs.contains("Comprovante de reserva nº 1\\nHóspede: Carla Dias (CPF 111.222.333-44)"), msgs);
        assertTrue(msgs.contains("[Cópia] Reserva confirmada"));

        var occ = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertTrue(occ.headers().firstValue("content-type").orElse("").contains("text/csv"));

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
        r = call("POST", "/reservations/1/check-out", null);
        assertTrue(r.body().contains("\"title\":\"Comprovante de reserva\""), r.body());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());

        var rev = call("GET", "/reports/revenue?month=2027-03", null).body();
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n", rev);
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao e json").statusCode());
        assertEquals(400, call("POST", "/reservations", body("101", "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", body("101", "2027-03-10", "2027-03-12", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", body("999", "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", body("101", "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/abc", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy", null).statusCode());
    }

    @Test
    void nonRefundableCancelKeepsMoney() throws Exception {
        call("POST", "/reservations", body("101", "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        var r = call("POST", "/reservations/1/cancellation", null);
        assertTrue(r.body().contains("\"refund\":0.00"), r.body());
        assertTrue(call("GET", "/reports/revenue?month=2027-07", null).body().contains(",CANCELLED,509.80\ntotal,509.80"));
        // quarto liberado novamente
        assertEquals(201, call("POST", "/reservations", body("101", "2027-07-10", "2027-07-12", 2, "STANDARD")).statusCode());
    }
}
