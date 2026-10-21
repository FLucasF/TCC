package pousada;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    Javalin app;
    HttpClient client = HttpClient.newHttpClient();

    @BeforeEach void start() { app = App.create(new Hotel(new Outbox())).start(0); }
    @AfterEach void stop() { app.stop(); }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    String reserve(int room, String in, String out, String plan, String extra) throws Exception {
        var r = call("POST", "/reservations", "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\","
                + "\"guestEmail\":\"c@x.com\",\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"" + in + "\",\"checkOut\":\""
                + out + "\",\"guests\":2,\"ratePlan\":\"" + plan + "\"" + extra + "}");
        return r.statusCode() + " " + r.body();
    }

    @Test void highSeasonStandard() throws Exception {
        String r = reserve(101, "2027-07-10", "2027-07-12", "STANDARD", "");
        assertTrue(r.startsWith("201"));
        assertTrue(r.contains("\"total\":599.76"), r);
    }

    @Test void corporate() throws Exception {
        String r = reserve(201, "2027-03-10", "2027-03-12", "CORPORATE", ",\"companyCode\":\"TECNOSUL\"");
        assertTrue(r.contains("\"subtotal\":779.80") && r.contains("\"discount\":93.58") && r.contains("\"total\":686.22"), r);
        assertTrue(reserve(202, "2027-03-10", "2027-03-12", "CORPORATE", "").startsWith("400"));
        assertTrue(reserve(202, "2027-03-10", "2027-03-12", "CORPORATE", ",\"companyCode\":\"X\"").startsWith("400"));
    }

    @Test void weekendPackageAndRefund() throws Exception {
        assertTrue(reserve(201, "2027-03-12", "2027-03-14", "WEEKEND_PACKAGE", "").contains("\"total\":820.00"));
        assertTrue(reserve(202, "2027-03-13", "2027-03-15", "WEEKEND_PACKAGE", "").startsWith("400"));
        call("POST", "/reservations/1/confirmation", null);
        assertTrue(call("POST", "/reservations/1/cancellation", null).body().contains("\"refund\":410.00"));
    }

    @Test void longStay() throws Exception {
        String r = reserve(101, "2027-03-01", "2027-03-08", "LONG_STAY", "");
        assertTrue(r.contains("\"subtotal\":1749.30") && r.contains("\"discount\":174.93") && r.contains("\"total\":1574.37"), r);
        assertTrue(reserve(102, "2027-03-01", "2027-03-07", "LONG_STAY", "").startsWith("400"));
    }

    @Test void confirmationSendsEmailAndReports() throws Exception {
        reserve(201, "2027-03-10", "2027-03-12", "STANDARD", "");
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/confirmation", null).statusCode());
        String msgs = call("GET", "/messages", null).body();
        assertTrue(msgs.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(msgs.contains("Total: R$ 779,80\\n\\nA entrada"), msgs);
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2027-03-11", null).body());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CONFIRMED,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
        assertEquals(409, reserve(201, "2027-03-11", "2027-03-13", "STANDARD", "").startsWith("409") ? 409 : 0);
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).statusCode());
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
    }
}
