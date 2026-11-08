package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pousada.api.Api;
import pousada.report.Reports;
import pousada.service.ReservationService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTest {
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();
    static final ObjectMapper json = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @BeforeAll
    static void start() {
        InMemoryRepository repo = new InMemoryRepository();
        app = new Api(new ReservationService(repo), new Reports(repo)).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() {
        app.stop();
    }

    record Res(int status, String body) {
        JsonNode json() throws Exception {
            return ApiTest.json.readTree(body);
        }
    }

    static Res send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body());
    }

    static String booking(int room, String in, String out, int guests, String plan, String extra) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@x.com\","
                + "\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out
                + "\",\"guests\":" + guests + ",\"ratePlan\":\"" + plan + "\"" + extra + "}";
    }

    @Test
    void fullFlowWithReceiptAndEmail() throws Exception {
        Res created = send("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD", ""));
        assertEquals(201, created.status());
        assertEquals("PENDING", created.json().get("status").asText());
        long id = created.json().get("id").asLong();

        assertEquals(409, send("POST", "/reservations/" + id + "/check-in", null).status());
        assertEquals(409, send("GET", "/reservations/" + id + "/document", null).status());

        Res confirmed = send("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, confirmed.status());
        assertTrue(confirmed.body().contains("\"amountPaid\":779.80"), confirmed.body());

        JsonNode message = null;
        for (JsonNode m : send("GET", "/messages", null).json()) {
            if (m.get("body").asText().contains("nº " + id + "\n")) {
                message = m;
            }
        }
        assertEquals("EMAIL", message.get("channel").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", message.get("subject").asText());
        assertTrue(message.get("body").asText().contains("Total: R$ 779,80\n\nA entrada é a partir das 14h"));

        assertEquals(200, send("POST", "/reservations/" + id + "/check-in", null).status());
        Res out = send("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals(409, send("POST", "/reservations/" + id + "/cancellation", null).status());
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, send("POST", "/reservations", "nao e json").status());
        assertEquals(400, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-10", 1, "STANDARD", "")).status());
        assertEquals(400, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 0, "STANDARD", "")).status());
        assertEquals(404, send("POST", "/reservations", booking(999, "2027-03-10", "2027-03-11", 1, "STANDARD", "")).status());
        assertEquals(422, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-11", 3, "STANDARD", "")).status());
        assertEquals(400, send("POST", "/reservations", booking(102, "2027-03-10", "2027-03-11", 1, "CORPORATE", "")).status());
        assertEquals(404, send("GET", "/reservations/9999", null).status());
        assertEquals(400, send("GET", "/reports/revenue?month=2027-13", null).status());
        assertEquals("RESERVATION_NOT_FOUND", send("GET", "/reservations/x", null).json().get("error").asText());
    }

    @Test
    void overlapAndCancellationRefund() throws Exception {
        long id = send("POST", "/reservations", booking(301, "2027-03-12", "2027-03-14", 2, "WEEKEND_PACKAGE", ""))
                .json().get("id").asLong();
        assertEquals(409, send("POST", "/reservations", booking(301, "2027-03-13", "2027-03-15", 2, "STANDARD", "")).status());
        send("POST", "/reservations/" + id + "/confirmation", null);
        String cancelled = send("POST", "/reservations/" + id + "/cancellation", null).body();
        assertTrue(cancelled.contains("\"refund\":600.00"), cancelled);
        assertEquals(201, send("POST", "/reservations", booking(301, "2027-03-13", "2027-03-15", 2, "STANDARD", "")).status());
    }

    @Test
    void reports() throws Exception {
        long id = send("POST", "/reservations", booking(102, "2027-09-01", "2027-09-03", 1, "NON_REFUNDABLE", ""))
                .json().get("id").asLong();
        send("POST", "/reservations/" + id + "/confirmation", null);
        String occupancy = send("GET", "/reports/occupancy?date=2027-09-02", null).body();
        assertEquals("room,type,reservation,check_in,check_out\n102,STANDARD," + id + ",2027-09-01,2027-09-03\ntotal,1/5\n", occupancy);
        assertTrue(send("GET", "/reports/occupancy?date=2027-09-03", null).body().endsWith("total,0/5\n"));
        send("POST", "/reservations/" + id + "/cancellation", null);
        String revenue = send("GET", "/reports/revenue?month=2027-09", null).body();
        assertEquals("reservation,room,check_in,check_out,status,amount\n" + id
                + ",102,2027-09-01,2027-09-03,CANCELLED,424.83\ntotal,424.83\n", revenue);
    }
}
