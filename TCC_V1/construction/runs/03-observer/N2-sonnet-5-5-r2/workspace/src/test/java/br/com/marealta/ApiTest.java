package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

/** Roda contra o PostgreSQL de DB_URL, num schema só deste teste. Pulado se DB_URL não existir. */
class ApiTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static Javalin app;
    private static String base;
    private static String schema;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "DB_URL não definido");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(url);
        ds.setUser(System.getenv("DB_USER"));
        ds.setPassword(System.getenv("DB_PASSWORD"));
        ds.setCurrentSchema(schema);
        app = Application.create(ds).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app == null) {
            return;
        }
        app.stop();
        try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")); Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.method(method, HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json");
        }
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String reservationBody(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com",
                 "guestCpf":"111.222.333-44","checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}"""
                .formatted(room, in, out, guests, plan);
    }

    private static JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", reservationBody(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return JSON.readTree(r.body());
    }

    private static void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, JSON.readTree(r.body()).get("error").asText());
    }

    @Test
    void listsRooms() throws Exception {
        HttpResponse<String> r = call("GET", "/rooms", null);
        assertEquals(200, r.statusCode());
        JsonNode rooms = JSON.readTree(r.body());
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertTrue(r.body().contains("\"nightlyRate\":249.90"), r.body());
    }

    @Test
    void validationAndErrors() throws Exception {
        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", reservationBody(101, "2027-05-10", "2027-05-10", 1, "STANDARD")),
                400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", reservationBody(101, "2027-05-10", "2027-05-11", 0, "STANDARD")),
                400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", reservationBody(101, "10/05/2027", "2027-05-11", 1, "STANDARD")),
                400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", reservationBody(999, "2027-05-10", "2027-05-11", 1, "STANDARD")),
                404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", reservationBody(101, "2027-05-10", "2027-05-11", 3, "STANDARD")),
                422, "CAPACITY_EXCEEDED");
        assertError(call("GET", "/reservations/99999", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("POST", "/reservations/99999/confirmation", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/occupancy?date=hoje", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue?month=2027-13", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
    }

    @Test
    void fullFlowWithMessagesAndReports() throws Exception {
        JsonNode created = create(202, "2030-07-10", "2030-07-12", 2, "STANDARD");
        long id = created.get("id").asLong();
        assertEquals("PENDING", created.get("status").asText());
        assertEquals(0, created.get("amountPaid").decimalValue().signum());
        assertTrue(created.get("refund").isNull());
        assertTrue(created.toString().contains("\"subtotal\":935.76"), created.toString());

        // mesmo quarto, noite sobreposta
        assertError(call("POST", "/reservations", reservationBody(202, "2030-07-11", "2030-07-13", 1, "STANDARD")),
                409, "ROOM_UNAVAILABLE");
        // noite seguinte à saída é livre
        create(202, "2030-07-12", "2030-07-13", 1, "STANDARD");

        assertError(call("POST", "/reservations/" + id + "/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("GET", "/reservations/" + id + "/document", null), 409, "OPERATION_NOT_ALLOWED");

        HttpResponse<String> confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, confirmed.statusCode());
        assertEquals("CONFIRMED", JSON.readTree(confirmed.body()).get("status").asText());
        assertError(call("POST", "/reservations/" + id + "/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        String messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(messages.contains("Reserva confirmada - quarto 202"));
        assertTrue(messages.contains("Pagamento recebido - reserva " + id));
        assertTrue(messages.contains("governanca@pousadamarealta.com.br"));
        assertTrue(messages.contains("financeiro@pousadamarealta.com.br"));
        assertTrue(messages.contains("\"channel\":\"EMAIL\""));

        HttpResponse<String> doc = call("GET", "/reservations/" + id + "/document", null);
        assertEquals(200, doc.statusCode());
        assertEquals("Comprovante de reserva", JSON.readTree(doc.body()).get("title").asText());

        String occupancy = call("GET", "/reports/occupancy?date=2030-07-11", null).body();
        assertEquals("room,type,reservation,check_in,check_out\n202,DELUXE," + id + ",2030-07-10,2030-07-12\ntotal,1/5\n",
                occupancy);
        // no dia da saída o quarto não conta
        assertTrue(call("GET", "/reports/occupancy?date=2030-07-12", null).body().endsWith("total,0/5\n"));

        HttpResponse<String> csv = call("GET", "/reports/occupancy?date=2030-07-11", null);
        assertEquals("text/csv; charset=utf-8", csv.headers().firstValue("Content-Type").orElse(""));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        assertError(call("POST", "/reservations/" + id + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        HttpResponse<String> out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals(200, out.statusCode());
        assertEquals(935.76, JSON.readTree(out.body()).get("total").asDouble());

        String revenue = call("GET", "/reports/revenue?month=2030-07", null).body();
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + id + ",202,2030-07-10,2030-07-12,CHECKED_OUT,935.76\ntotal,935.76\n", revenue);
    }

    @Test
    void cancellationRulesAndNotifications() throws Exception {
        // pendente: cancela sem avisar ninguém
        long before = JSON.readTree(call("GET", "/messages", null).body()).size();
        long pending = create(101, "2031-03-10", "2031-03-11", 1, "STANDARD").get("id").asLong();
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/" + pending + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(0, cancelled.get("refund").decimalValue().signum());
        assertEquals(before, JSON.readTree(call("GET", "/messages", null).body()).size());
        assertError(call("POST", "/reservations/" + pending + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        // cancelada libera o quarto
        long again = create(101, "2031-03-10", "2031-03-11", 1, "STANDARD").get("id").asLong();

        // confirmada no plano normal: reembolso integral
        call("POST", "/reservations/" + again + "/confirmation", null);
        JsonNode refunded = JSON.readTree(call("POST", "/reservations/" + again + "/cancellation", null).body());
        assertEquals(249.90, refunded.get("refund").asDouble());
        String messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reembolso - reserva " + again));
        assertTrue(messages.contains("Reserva cancelada - quarto 101"));

        // não reembolsável: retém tudo e não avisa o financeiro
        long nr = create(102, "2031-03-10", "2031-03-11", 1, "NON_REFUNDABLE").get("id").asLong();
        call("POST", "/reservations/" + nr + "/confirmation", null);
        JsonNode kept = JSON.readTree(call("POST", "/reservations/" + nr + "/cancellation", null).body());
        assertEquals(0, kept.get("refund").decimalValue().signum());
        assertTrue(!call("GET", "/messages", null).body().contains("Reembolso - reserva " + nr));

        String revenue = call("GET", "/reports/revenue?month=2031-03", null).body();
        assertTrue(revenue.contains(nr + ",102,2031-03-10,2031-03-11,CANCELLED,212.42\n"), revenue);
        assertTrue(!revenue.contains("\n" + again + ","), revenue);
        assertTrue(revenue.endsWith("total,212.42\n"), revenue);
    }
}
