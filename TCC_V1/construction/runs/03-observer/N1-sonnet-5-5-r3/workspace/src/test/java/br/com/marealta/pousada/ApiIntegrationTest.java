package br.com.marealta.pousada;

import br.com.marealta.pousada.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Roda contra o PostgreSQL de DB_URL, num schema próprio; é pulado se DB_URL não existir. */
class ApiIntegrationTest {

    static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static Javalin app;
    static String base;
    static String schema;
    static String url;

    @BeforeAll
    static void start() throws Exception {
        String dbUrl = System.getenv("DB_URL");
        assumeTrue(dbUrl != null && !dbUrl.isBlank(), "DB_URL não definido");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(dbUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        url = dbUrl + (dbUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        Database db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.initialize();
        app = Main.start(db, 0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    static JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return JSON.readTree(r.body());
    }

    @Test
    void fullFlowWithNotificationsAndReports() throws Exception {
        JsonNode r = create(201, "2031-03-10", "2031-03-12", 2, "STANDARD");
        long id = r.get("id").asLong();
        assertEquals("779.80", r.get("total").decimalValue().toPlainString());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("refund").isNull());

        int before = JSON.readTree(call("GET", "/messages", null).body()).size();
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        JsonNode confirmed = JSON.readTree(call("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("779.80", confirmed.get("amountPaid").decimalValue().toPlainString());
        JsonNode messages = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(before + 3, messages.size());

        String occupancy = call("GET", "/reports/occupancy?date=2031-03-11", null).body();
        assertTrue(occupancy.contains("201,DELUXE," + id + ",2031-03-10,2031-03-12\n"), occupancy);
        assertTrue(call("GET", "/reports/occupancy?date=2031-03-12", null).body().lines().noneMatch(l -> l.startsWith("201,")));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        JsonNode receipt = JSON.readTree(call("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals(id, receipt.get("reservationId").asLong());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());

        HttpResponse<String> revenue = call("GET", "/reports/revenue?month=2031-03", null);
        assertTrue(revenue.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertTrue(revenue.body().contains(id + ",201,2031-03-10,2031-03-12,CHECKED_OUT,779.80"), revenue.body());
    }

    @Test
    void cancelRefundableAfterPaymentNotifiesHousekeepingAndFinance() throws Exception {
        long id = create(202, "2031-05-10", "2031-05-11", 1, "STANDARD").get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        int before = JSON.readTree(call("GET", "/messages", null).body()).size();
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals("389.90", cancelled.get("refund").decimalValue().toPlainString());
        assertEquals(before + 2, JSON.readTree(call("GET", "/messages", null).body()).size());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        // noite liberada
        create(202, "2031-05-10", "2031-05-11", 1, "STANDARD");
    }

    @Test
    void cancelPendingNotifiesNobodyAndNonRefundableKeepsMoney() throws Exception {
        long pending = create(101, "2031-06-01", "2031-06-02", 1, "STANDARD").get("id").asLong();
        int before = JSON.readTree(call("GET", "/messages", null).body()).size();
        call("POST", "/reservations/" + pending + "/cancellation", null);
        assertEquals(before, JSON.readTree(call("GET", "/messages", null).body()).size());

        long nr = create(102, "2031-06-01", "2031-06-02", 1, "NON_REFUNDABLE").get("id").asLong();
        call("POST", "/reservations/" + nr + "/confirmation", null);
        before = JSON.readTree(call("GET", "/messages", null).body()).size();
        JsonNode c = JSON.readTree(call("POST", "/reservations/" + nr + "/cancellation", null).body());
        assertEquals("0.00", c.get("refund").decimalValue().toPlainString());
        assertEquals(before + 1, JSON.readTree(call("GET", "/messages", null).body()).size());
        String revenue = call("GET", "/reports/revenue?month=2031-06", null).body();
        assertTrue(revenue.contains(nr + ",102,2031-06-01,2031-06-02,CANCELLED,212.42"), revenue);
        assertFalse(revenue.contains(pending + ",101"), revenue);
    }

    @Test
    void errors() throws Exception {
        create(301, "2031-09-01", "2031-09-03", 4, "STANDARD");
        assertEquals(409, call("POST", "/reservations", booking(301, "2031-09-02", "2031-09-04", 1, "STANDARD")).statusCode());
        assertEquals("ROOM_UNAVAILABLE", JSON.readTree(call("POST", "/reservations",
                booking(301, "2031-08-31", "2031-09-02", 1, "STANDARD")).body()).get("error").asText());
        assertEquals(201, call("POST", "/reservations", booking(301, "2031-09-03", "2031-09-04", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", booking(101, "2031-10-01", "2031-10-02", 3, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", booking(999, "2031-10-01", "2031-10-02", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2031-10-02", "2031-10-02", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "2031-10-01", "2031-10-02", 0, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "não é json").statusCode());
        assertEquals(400, call("POST", "/reservations", "{}").statusCode());
        assertEquals(404, call("GET", "/reservations/999999", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2031-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy?date=ontem", null).statusCode());
        JsonNode rooms = JSON.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals("249.90", rooms.get(0).get("nightlyRate").decimalValue().toPlainString());
    }
}
