package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pousada.domain.RoomCatalog;
import pousada.persistence.Database;
import pousada.report.OccupancyReport;
import pousada.report.RatePlanReport;
import pousada.report.ReportService;
import pousada.report.RevenueReport;
import pousada.report.TourismTaxReport;
import pousada.service.ReservationService;
import pousada.web.Api;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Roda contra o PostgreSQL de DB_URL, num schema só dele que some no fim. */
class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static Database database;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null && !url.isBlank(), "DB_URL não definido");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        exec(url, "CREATE SCHEMA " + schema);
        database = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        database.createSchema();
        RoomCatalog rooms = RoomCatalog.standard();
        app = new Api(new ReservationService(database, rooms), new ReportService(database), List.of(
                new OccupancyReport(rooms), new RevenueReport(),
                new TourismTaxReport(new BigDecimal("4.50")), new RatePlanReport())).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (database != null) database.close();
        if (schema != null) exec(System.getenv("DB_URL"), "DROP SCHEMA " + schema + " CASCADE");
    }

    private static void exec(String url, String sql) throws Exception {
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private static HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        return HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private static long create(String body) throws Exception {
        HttpResponse<String> r = send("POST", "/reservations", body);
        assertEquals(201, r.statusCode(), r.body());
        return JSON.readTree(r.body()).get("id").asLong();
    }

    private static void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, JSON.readTree(r.body()).get("error").asText());
    }

    @Test
    void fullFlowWithConfirmationEmailAndReports() throws Exception {
        long id = create(booking(201, "2031-03-10", "2031-03-12", 2, "STANDARD"));
        JsonNode created = JSON.readTree(send("GET", "/reservations/" + id, null).body());
        assertEquals("PENDING", created.get("status").asText());
        assertEquals("779.80", created.get("total").asText());
        assertTrue(created.get("refund").isNull());

        assertError(send("GET", "/reservations/" + id + "/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(send("POST", "/reservations", booking(201, "2031-03-11", "2031-03-13", 2, "STANDARD")),
                409, "ROOM_UNAVAILABLE");

        JsonNode confirmed = JSON.readTree(send("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("779.80", confirmed.get("amountPaid").asText());

        JsonNode mine = null;
        for (JsonNode m : JSON.readTree(send("GET", "/messages", null).body())) {
            if (m.get("body").asText().contains("Comprovante de reserva nº " + id + "\n")) mine = m;
        }
        assertTrue(mine != null, "e-mail de confirmação não encontrado");
        assertEquals("EMAIL", mine.get("channel").asText());
        assertEquals("carla@x.com", mine.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", mine.get("subject").asText());
        assertTrue(mine.get("body").asText().contains("Total: R$ 779,80"));

        assertEquals(200, send("POST", "/reservations/" + id + "/check-in", null).statusCode());
        JsonNode receipt = JSON.readTree(send("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals(receipt, JSON.readTree(send("GET", "/reservations/" + id + "/document", null).body()));

        HttpResponse<String> occupancy = send("GET", "/reports/occupancy?date=2031-03-11", null);
        assertEquals("text/csv; charset=utf-8", occupancy.headers().firstValue("content-type").orElse(""));
        assertTrue(occupancy.body().startsWith("Pousada Maré Alta,occupancy,2031-03-11\n"));
        assertTrue(occupancy.body().endsWith("total,1/5\n"));
        assertTrue(send("GET", "/reports/tourism-tax?month=2031-03", null).body().endsWith("total,18.00\n"));
        assertError(send("GET", "/reports/revenue?month=2031-3", null), 400, "INVALID_REQUEST");
    }

    @Test
    void cancellationRefundsOnlyTheStandardPlan() throws Exception {
        long standard = create(booking(101, "2032-05-10", "2032-05-11", 1, "STANDARD"));
        long locked = create(booking(102, "2032-05-10", "2032-05-11", 1, "NON_REFUNDABLE"));
        send("POST", "/reservations/" + standard + "/confirmation", null);
        send("POST", "/reservations/" + locked + "/confirmation", null);
        JsonNode a = JSON.readTree(send("POST", "/reservations/" + standard + "/cancellation", null).body());
        JsonNode b = JSON.readTree(send("POST", "/reservations/" + locked + "/cancellation", null).body());
        assertEquals("249.90", a.get("refund").asText());
        assertEquals("0.00", b.get("refund").asText());
        assertError(send("POST", "/reservations/" + standard + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        // o quarto cancelado volta a ficar livre
        create(booking(101, "2032-05-10", "2032-05-11", 1, "STANDARD"));
        String revenue = send("GET", "/reports/revenue?month=2032-05", null).body();
        assertTrue(revenue.contains("CANCELLED,212.42"), revenue);
        assertTrue(revenue.endsWith("total,212.42\n"), revenue);
        assertTrue(send("GET", "/reports/rate-plans?month=2032-05", null).body()
                .endsWith("STANDARD,0,0.00\nNON_REFUNDABLE,1,212.42\ntotal,1,212.42\n"));
    }

    @Test
    void invalidRequests() throws Exception {
        assertError(send("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", booking(101, "2033-01-10", "2033-01-10", 1, "STANDARD")),
                400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", booking(101, "2033-01-10", "2033-01-11", 0, "STANDARD")),
                400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", booking(101, "2033-01-10", "2033-01-11", 1, "OTHER")),
                400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", "{\"roomNumber\":101}"), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", booking(999, "2033-01-10", "2033-01-11", 1, "STANDARD")),
                404, "ROOM_NOT_FOUND");
        assertError(send("POST", "/reservations", booking(101, "2033-01-10", "2033-01-11", 3, "STANDARD")),
                422, "CAPACITY_EXCEEDED");
        assertError(send("GET", "/reservations/999999", null), 404, "RESERVATION_NOT_FOUND");
        assertError(send("POST", "/reservations/999999/confirmation", null), 404, "RESERVATION_NOT_FOUND");
    }

    @Test
    void listsTheFiveRooms() throws Exception {
        JsonNode rooms = JSON.readTree(send("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals("579.90", rooms.get(4).get("nightlyRate").asText());
    }
}
