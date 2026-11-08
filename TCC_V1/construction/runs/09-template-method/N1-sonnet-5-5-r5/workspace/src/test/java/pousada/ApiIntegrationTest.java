package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;

import io.javalin.Javalin;
import pousada.db.Database;
import pousada.db.Schema;
import pousada.web.App;
import pousada.web.Json;

/** Usa o PostgreSQL de DB_URL, mas num schema próprio que some no fim. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ApiIntegrationTest {
    static String schema = "t_" + UUID.randomUUID().toString().replace("-", "");
    static Database db;
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        try (var c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             var s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        db = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        Schema.create(db);
        app = App.create(db).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        app.stop();
        db.close();
        try (var c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             var s = c.createStatement()) {
            s.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    static HttpResponse<String> send(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String body(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    static JsonNode json(HttpResponse<String> r) throws Exception {
        return Json.MAPPER.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true)).readTree(r.body());
    }

    @Test
    void roomsAreListed() throws Exception {
        var r = send("GET", "/rooms", null);
        assertEquals(200, r.statusCode());
        assertEquals(5, json(r).size());
        assertEquals(101, json(r).get(0).get("number").asInt());
        assertEquals(249.90, json(r).get(0).get("nightlyRate").asDouble());
    }

    @Test
    void fullLifecycleWithReceiptEmailAndReports() throws Exception {
        var created = send("POST", "/reservations", body(201, "2031-03-10", "2031-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        long id = json(created).get("id").asLong();
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertEquals("PENDING", json(created).get("status").asText());
        assertEquals("779.80", json(created).get("total").decimalValue().toPlainString());
        assertTrue(json(created).get("refund").isNull());

        assertEquals(409, send("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, send("POST", "/reservations/" + id + "/check-in", null).statusCode());

        var confirmed = send("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, confirmed.statusCode());
        assertEquals("779.80", json(confirmed).get("amountPaid").decimalValue().toPlainString());
        assertEquals(409, send("POST", "/reservations/" + id + "/confirmation", null).statusCode());

        var messages = json(send("GET", "/messages", null));
        var mail = messages.get(messages.size() - 1);
        assertEquals("EMAIL", mail.get("channel").asText());
        assertEquals("carla@example.com", mail.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", mail.get("subject").asText());
        assertTrue(mail.get("body").asText().contains("Comprovante de reserva nº " + id + "\nHóspede: Carla Dias"));

        var occupancy = send("GET", "/reports/occupancy?date=2031-03-10", null);
        assertEquals(200, occupancy.statusCode());
        assertEquals("text/csv; charset=utf-8", occupancy.headers().firstValue("content-type").orElse(""));
        assertEquals("Pousada Maré Alta,occupancy,2031-03-10\nroom,type,reservation,check_in,check_out\n"
                + "201,DELUXE," + id + ",2031-03-10,2031-03-12\ntotal,1/5\n", occupancy.body());
        assertTrue(send("GET", "/reports/occupancy?date=2031-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals(200, send("POST", "/reservations/" + id + "/check-in", null).statusCode());
        var receipt = send("POST", "/reservations/" + id + "/check-out", null);
        assertEquals(200, receipt.statusCode());
        assertEquals("Comprovante de reserva", json(receipt).get("title").asText());
        assertEquals("DELUXE", json(receipt).get("roomType").asText());
        assertEquals(200, send("GET", "/reservations/" + id + "/document", null).statusCode());

        assertEquals("Pousada Maré Alta,revenue,2031-03\nreservation,room,check_in,check_out,status,amount\n"
                + id + ",201,2031-03-10,2031-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                send("GET", "/reports/revenue?month=2031-03", null).body());
        assertEquals("Pousada Maré Alta,tourism-tax,2031-03\nreservation,room,check_in,check_out,guests,nights,tax\n"
                + id + ",201,2031-03-10,2031-03-12,2,2,18.00\ntotal,18.00\n",
                send("GET", "/reports/tourism-tax?month=2031-03", null).body());
        assertEquals("Pousada Maré Alta,rate-plans,2031-03\nrate_plan,reservations,amount\n"
                + "STANDARD,1,779.80\nNON_REFUNDABLE,0,0.00\ntotal,1,779.80\n",
                send("GET", "/reports/rate-plans?month=2031-03", null).body());
    }

    @Test
    void cancellationRefundsOnlyTheStandardPlan() throws Exception {
        long refundable = create(101, "2032-07-01", "2032-07-03", "STANDARD");
        long nonRefundable = create(102, "2032-07-01", "2032-07-03", "NON_REFUNDABLE");
        long pending = create(202, "2032-07-01", "2032-07-03", "NON_REFUNDABLE");
        for (long id : new long[] {refundable, nonRefundable}) {
            send("POST", "/reservations/" + id + "/confirmation", null);
        }
        assertEquals("599.76", json(send("POST", "/reservations/" + refundable + "/cancellation", null)).get("refund").decimalValue().toPlainString());
        var nr = json(send("POST", "/reservations/" + nonRefundable + "/cancellation", null));
        assertEquals("0.00", nr.get("refund").decimalValue().toPlainString());
        assertEquals("509.80", nr.get("amountPaid").decimalValue().toPlainString());
        assertEquals("0.00", json(send("POST", "/reservations/" + pending + "/cancellation", null)).get("refund").decimalValue().toPlainString());
        assertEquals(409, send("POST", "/reservations/" + pending + "/cancellation", null).statusCode());

        assertEquals("Pousada Maré Alta,revenue,2032-07\nreservation,room,check_in,check_out,status,amount\n"
                + nonRefundable + ",102,2032-07-01,2032-07-03,CANCELLED,509.80\ntotal,509.80\n",
                send("GET", "/reports/revenue?month=2032-07", null).body());
        // cancelada libera o quarto
        assertEquals(201, send("POST", "/reservations", body(101, "2032-07-01", "2032-07-03", 1, "STANDARD")).statusCode());
    }

    long create(int room, String in, String out, String plan) throws Exception {
        return json(send("POST", "/reservations", body(room, in, out, 1, plan))).get("id").asLong();
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        assertError(400, "INVALID_REQUEST", send("POST", "/reservations", "not json"));
        assertError(400, "INVALID_REQUEST", send("POST", "/reservations", "{}"));
        assertError(400, "INVALID_REQUEST", send("POST", "/reservations", body(101, "2033-01-05", "2033-01-05", 1, "STANDARD")));
        assertError(400, "INVALID_REQUEST", send("POST", "/reservations", body(101, "2033-01-05", "2033-01-06", 0, "STANDARD")));
        assertError(400, "INVALID_REQUEST", send("POST", "/reservations", body(101, "05/01/2033", "2033-01-06", 1, "STANDARD")));
        assertError(404, "ROOM_NOT_FOUND", send("POST", "/reservations", body(999, "2033-01-05", "2033-01-06", 1, "STANDARD")));
        assertError(422, "CAPACITY_EXCEEDED", send("POST", "/reservations", body(101, "2033-01-05", "2033-01-06", 3, "STANDARD")));
        assertError(404, "RESERVATION_NOT_FOUND", send("GET", "/reservations/999999", null));
        assertError(404, "RESERVATION_NOT_FOUND", send("POST", "/reservations/999999/confirmation", null));
        assertError(400, "INVALID_REQUEST", send("GET", "/reports/revenue?month=2033-1", null));
        assertError(400, "INVALID_REQUEST", send("GET", "/reports/rate-plans", null));
        assertError(400, "INVALID_REQUEST", send("GET", "/reports/occupancy?date=2033-02-30", null));
    }

    @Test
    void doubleBookingIsRejectedButBackToBackStaysAreFine() throws Exception {
        assertEquals(201, send("POST", "/reservations", body(301, "2034-05-10", "2034-05-12", 4, "STANDARD")).statusCode());
        assertError(409, "ROOM_UNAVAILABLE", send("POST", "/reservations", body(301, "2034-05-11", "2034-05-13", 1, "STANDARD")));
        assertEquals(201, send("POST", "/reservations", body(301, "2034-05-12", "2034-05-13", 1, "STANDARD")).statusCode());
    }

    static void assertError(int status, String code, HttpResponse<String> r) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, json(r).get("error").asText());
    }
}
