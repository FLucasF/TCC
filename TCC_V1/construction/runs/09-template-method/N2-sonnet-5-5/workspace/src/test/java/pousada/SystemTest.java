package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pousada.db.Database;
import pousada.domain.Money;
import pousada.domain.Pricing;
import pousada.domain.RoomCatalog;

class SystemTest {
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        Database db = new Database("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
        db.createSchema();
        app = Main.build(db).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() {
        app.stop();
    }

    static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String reservation(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@ex.com","guestCpf":"11122233344",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void pricing() {
        var room = new RoomCatalog().get(101);
        var q = Pricing.quote(room, java.time.LocalDate.parse("2027-07-10"), java.time.LocalDate.parse("2027-07-12"),
                pousada.domain.RatePlan.STANDARD);
        assertEquals(Money.of("599.76"), q.total());
        var nr = Pricing.quote(room, java.time.LocalDate.parse("2027-03-10"), java.time.LocalDate.parse("2027-03-11"),
                pousada.domain.RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("37.48"), nr.discount());
        assertEquals(Money.of("212.42"), nr.total());
    }

    @Test
    void fullFlowAndReports() throws Exception {
        var created = call("POST", "/reservations", reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertTrue(created.body().contains("\"refund\":null"));

        assertEquals(409, call("POST", "/reservations", reservation(201, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", reservation(101, "2027-03-11", "2027-03-13", 3, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "nao e json").statusCode());
        assertEquals(404, call("POST", "/reservations", reservation(999, "2027-03-11", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());

        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        var messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(messages.contains("Diárias: R$ 779,80"), messages);

        var doc = call("GET", "/reservations/1/document", null).body();
        assertTrue(doc.contains("\"title\":\"Comprovante de reserva\""));

        var occupancy = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertEquals("Pousada Maré Alta,occupancy,2027-03-10\nroom,type,reservation,check_in,check_out\n"
                + "201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occupancy.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));

        call("POST", "/reservations", reservation(301, "2027-03-05", "2027-03-08", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/2/confirmation", null);
        var cancelled = call("POST", "/reservations/2/cancellation", null);
        assertTrue(cancelled.body().contains("\"refund\":0.00"), cancelled.body());

        assertEquals("Pousada Maré Alta,revenue,2027-03\nreservation,room,check_in,check_out,status,amount\n"
                        + "2,301,2027-03-05,2027-03-08,CANCELLED,1478.74\n1,201,2027-03-10,2027-03-12,CONFIRMED,779.80\n"
                        + "total,2258.54\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
        assertEquals("Pousada Maré Alta,tourism-tax,2027-03\nreservation,room,check_in,check_out,guests,nights,tax\n"
                        + "1,201,2027-03-10,2027-03-12,2,2,18.00\ntotal,18.00\n",
                call("GET", "/reports/tourism-tax?month=2027-03", null).body());
        assertEquals("Pousada Maré Alta,rate-plans,2027-03\nrate_plan,reservations,amount\n"
                        + "STANDARD,1,779.80\nNON_REFUNDABLE,1,1478.74\ntotal,2,2258.54\n",
                call("GET", "/reports/rate-plans?month=2027-03", null).body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).statusCode());

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        assertTrue(call("POST", "/reservations/1/check-out", null).body().contains("\"nights\":2"));
        assertEquals(404, call("GET", "/reservations/abc", null).statusCode());
    }
}
