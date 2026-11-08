package com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marealta.persistence.Database;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Teste de ponta a ponta contra o PostgreSQL de DB_URL, DB_USER e DB_PASSWORD. */
class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() {
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        app = App.start(db, 0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() {
        app.stop();
    }

    @BeforeEach
    void clean() throws Exception {
        try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")); Statement s = c.createStatement()) {
            s.execute("TRUNCATE reservations, messages RESTART IDENTITY");
        }
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode());
        return JSON.readTree(r.body());
    }

    private void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode());
        assertEquals(code, JSON.readTree(r.body()).get("error").asText());
    }

    @Test
    void roomsAreListed() throws Exception {
        JsonNode rooms = JSON.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble());
    }

    @Test
    void fullLifecycleWithNotifications() throws Exception {
        JsonNode r = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(call("GET", "/reservations/1", null).body().contains("\"amountPaid\":0.00"));
        assertTrue(call("GET", "/reservations/1", null).body().contains("\"refund\":null"));
        assertTrue(call("GET", "/reservations/1", null).body().contains("\"total\":779.80"));

        assertError(call("GET", "/reservations/1/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations/1/check-in", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode confirmed = JSON.readTree(call("POST", "/reservations/1/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(779.80, confirmed.get("amountPaid").asDouble());
        assertError(call("POST", "/reservations/1/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode messages = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(3, messages.size());
        assertEquals("EMAIL", messages.get(0).get("channel").asText());

        HttpResponse<String> occupancy = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                occupancy.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        JsonNode receipt = JSON.readTree(call("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals(1, receipt.get("reservationId").asInt());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertError(call("POST", "/reservations/1/cancellation", null), 409, "OPERATION_NOT_ALLOWED");

        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void cancellationNotifiesAndRefundsByPlan() throws Exception {
        create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");
        call("POST", "/reservations/1/confirmation", null);
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/1/cancellation", null).body());
        assertEquals(599.76, cancelled.get("refund").asDouble());
        JsonNode messages = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(5, messages.size());

        create(101, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE");
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/cancellation", null);
        assertEquals(5 + 3 + 1, JSON.readTree(call("GET", "/messages", null).body()).size());
        assertEquals("reservation,room,check_in,check_out,status,amount\n2,101,2027-07-10,2027-07-12,CANCELLED,509.80\ntotal,509.80\n",
                call("GET", "/reports/revenue?month=2027-07", null).body());

        create(102, "2027-07-10", "2027-07-12", 2, "STANDARD");
        int before = JSON.readTree(call("GET", "/messages", null).body()).size();
        call("POST", "/reservations/3/cancellation", null);
        assertEquals(before, JSON.readTree(call("GET", "/messages", null).body()).size());
    }

    @Test
    void validationAndConflicts() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        assertEquals(201, call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-13", 1, "STANDARD")).statusCode());
        assertError(call("POST", "/reservations", booking(101, "2027-04-10", "2027-04-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("POST", "/reservations", booking(999, "2027-04-10", "2027-04-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(101, "2027-04-12", "2027-04-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-04-10", "2027-04-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-4-10", "2027-04-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reservations/99", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/occupancy?date=ontem", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue?month=2027-13", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
    }

    @Test
    void cancelledReservationFreesTheRoom() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        call("POST", "/reservations/1/cancellation", null);
        assertEquals(201, call("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
    }
}
