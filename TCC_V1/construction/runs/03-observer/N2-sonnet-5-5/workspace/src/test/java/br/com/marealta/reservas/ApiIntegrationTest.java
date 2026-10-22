package br.com.marealta.reservas;

import br.com.marealta.reservas.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Roda contra o PostgreSQL de DB_URL, num schema próprio que é apagado no fim. */
class ApiIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static String baseUrl;
    private static Javalin app;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null, "DB_URL não definido");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        String schemaUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        app = Main.build(new Database(schemaUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        app.start(0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String reservation(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com",
                 "guestCpf":"111.222.333-44","checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}"""
                .formatted(room, in, out, guests, plan);
    }

    private static JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", reservation(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return MAPPER.readTree(r.body());
    }

    private static String errorOf(HttpResponse<String> r) throws Exception {
        return MAPPER.readTree(r.body()).get("error").asText();
    }

    @Test
    void roomsAreListed() throws Exception {
        JsonNode rooms = MAPPER.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(389.90, rooms.get(2).get("nightlyRate").asDouble());
    }

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() throws Exception {
        JsonNode created = create(201, "2030-03-10", "2030-03-12", 2, "STANDARD");
        long id = created.get("id").asLong();
        assertEquals("PENDING", created.get("status").asText());
        assertTrue(created.get("refund").isNull());
        int before = MAPPER.readTree(call("GET", "/messages", null).body()).size();

        JsonNode confirmed = MAPPER.readTree(call("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(779.80, confirmed.get("amountPaid").asDouble());

        JsonNode messages = MAPPER.readTree(call("GET", "/messages", null).body());
        assertEquals(before + 3, messages.size());
        String all = messages.toString();
        assertTrue(all.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(all.contains("governanca@pousadamarealta.com.br"));
        assertTrue(all.contains("Reserva confirmada - quarto 201"));
        assertTrue(all.contains("financeiro@pousadamarealta.com.br"));
        assertTrue(all.contains("Pagamento recebido - reserva " + id));

        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).statusCode());
    }

    @Test
    void cancellationRules() throws Exception {
        long pending = create(202, "2030-04-10", "2030-04-12", 2, "STANDARD").get("id").asLong();
        int before = MAPPER.readTree(call("GET", "/messages", null).body()).size();
        JsonNode cancelled = MAPPER.readTree(call("POST", "/reservations/" + pending + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(0.0, cancelled.get("refund").asDouble());
        assertEquals(before, MAPPER.readTree(call("GET", "/messages", null).body()).size());

        long paid = create(202, "2030-04-10", "2030-04-12", 2, "STANDARD").get("id").asLong();
        call("POST", "/reservations/" + paid + "/confirmation", null);
        before = MAPPER.readTree(call("GET", "/messages", null).body()).size();
        call("POST", "/reservations/" + paid + "/cancellation", null);
        String all = MAPPER.readTree(call("GET", "/messages", null).body()).toString();
        assertEquals(before + 2, MAPPER.readTree(call("GET", "/messages", null).body()).size());
        assertTrue(all.contains("Reserva cancelada - quarto 202"));
        assertTrue(all.contains("Reembolso - reserva " + paid));

        long locked = create(202, "2030-05-10", "2030-05-12", 2, "NON_REFUNDABLE").get("id").asLong();
        call("POST", "/reservations/" + locked + "/confirmation", null);
        before = MAPPER.readTree(call("GET", "/messages", null).body()).size();
        JsonNode c = MAPPER.readTree(call("POST", "/reservations/" + locked + "/cancellation", null).body());
        assertEquals(0.0, c.get("refund").asDouble());
        assertEquals(before + 1, MAPPER.readTree(call("GET", "/messages", null).body()).size());
        assertEquals(409, call("POST", "/reservations/" + locked + "/cancellation", null).statusCode());
    }

    @Test
    void errorsAndLifecycleAndReports() throws Exception {
        assertEquals(400, call("POST", "/reservations", "not json").statusCode());
        assertEquals("INVALID_REQUEST",
                errorOf(call("POST", "/reservations", reservation(101, "2031-01-12", "2031-01-10", 1, "STANDARD"))));
        assertEquals("INVALID_REQUEST",
                errorOf(call("POST", "/reservations", reservation(101, "2031-01-10", "2031-01-12", 0, "STANDARD"))));
        assertEquals("ROOM_NOT_FOUND",
                errorOf(call("POST", "/reservations", reservation(999, "2031-01-10", "2031-01-12", 1, "STANDARD"))));
        assertEquals("CAPACITY_EXCEEDED",
                errorOf(call("POST", "/reservations", reservation(101, "2031-01-10", "2031-01-12", 3, "STANDARD"))));
        assertEquals("RESERVATION_NOT_FOUND", errorOf(call("GET", "/reservations/99999", null)));

        long id = create(101, "2031-01-10", "2031-01-12", 2, "NON_REFUNDABLE").get("id").asLong();
        assertEquals("ROOM_UNAVAILABLE",
                errorOf(call("POST", "/reservations", reservation(101, "2031-01-11", "2031-01-13", 1, "STANDARD"))));
        create(101, "2031-01-12", "2031-01-13", 1, "STANDARD"); // sai num dia, entra no mesmo

        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());

        String occupancy = call("GET", "/reports/occupancy?date=2031-01-11", null).body();
        assertTrue(occupancy.startsWith("room,type,reservation,check_in,check_out\n101,STANDARD," + id
                + ",2031-01-10,2031-01-12\ntotal,1/5"), occupancy);
        assertTrue(call("GET", "/reports/occupancy?date=2031-01-12", null).body().contains("total,0/5"));
        assertEquals(400, call("GET", "/reports/occupancy?date=2031-1-12", null).statusCode());

        call("POST", "/reservations/" + id + "/check-in", null);
        JsonNode receipt = MAPPER.readTree(call("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals(0.15 * 2 * 299.88, receipt.get("discount").asDouble(), 0.01);

        String revenue = call("GET", "/reports/revenue?month=2031-01", null).body();
        assertTrue(revenue.startsWith("reservation,room,check_in,check_out,status,amount\n" + id
                + ",101,2031-01-10,2031-01-12,CHECKED_OUT,509.80\ntotal,509.80"), revenue);
        assertEquals(400, call("GET", "/reports/revenue?month=2031-13", null).statusCode());
    }
}
