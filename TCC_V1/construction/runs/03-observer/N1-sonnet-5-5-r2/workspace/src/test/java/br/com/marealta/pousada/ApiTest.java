package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.pousada.persistence.Database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Ponta a ponta contra o PostgreSQL de DB_URL; pula se o banco não estiver configurado. */
class ApiTest {

    static Database db;
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();
    static final ObjectMapper json = new ObjectMapper();
    // ano aleatório distante, para não colidir com dados de execuções anteriores
    static final int YEAR = 3000 + ThreadLocalRandom.current().nextInt(6000);

    @BeforeAll
    static void start() {
        String url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null, "DB_URL ausente");
        db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        app = Main.start(db, 0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() {
        if (app != null) app.stop();
        if (db != null) db.close();
    }

    @Test
    void fullFlow() throws Exception {
        String body = booking(201, "03-10", "03-12", 2, "STANDARD");
        HttpResponse<String> created = send("POST", "/reservations", body);
        assertEquals(201, created.statusCode());
        JsonNode r = json.readTree(created.body());
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(779.80, r.get("total").asDouble());
        assertTrue(r.get("refund").isNull());

        assertEquals("ROOM_UNAVAILABLE", errorOf(send("POST", "/reservations", booking(201, "03-11", "03-13", 2, "STANDARD"))));
        assertEquals("OPERATION_NOT_ALLOWED", errorOf(send("GET", "/reservations/" + id + "/document", null)));
        assertEquals(200, send("POST", "/reservations/" + id + "/confirmation", null).statusCode());
        assertEquals("OPERATION_NOT_ALLOWED", errorOf(send("POST", "/reservations/" + id + "/confirmation", null)));

        HttpResponse<String> doc = send("GET", "/reservations/" + id + "/document", null);
        assertEquals(779.80, json.readTree(doc.body()).get("total").asDouble());

        String occupancy = send("GET", "/reports/occupancy?date=" + YEAR + "-03-10", null).body();
        assertTrue(occupancy.startsWith("room,type,reservation,check_in,check_out\n"));
        assertTrue(occupancy.contains("201,DELUXE," + id + "," + YEAR + "-03-10," + YEAR + "-03-12\n"));

        send("POST", "/reservations/" + id + "/check-in", null);
        HttpResponse<String> out = send("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", json.readTree(out.body()).get("title").asText());

        HttpResponse<String> revenue = send("GET", "/reports/revenue?month=" + YEAR + "-03", null);
        assertEquals("text/csv; charset=utf-8", revenue.headers().firstValue("Content-Type").orElse(""));
        assertTrue(revenue.body().endsWith("total,779.80\n"));
    }

    @Test
    void cancellationAndMessages() throws Exception {
        long id = json.readTree(send("POST", "/reservations",
                booking(301, "05-01", "05-03", 4, "NON_REFUNDABLE").replace("Carla", "Beto")).body()).get("id").asLong();
        send("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode cancelled = json.readTree(send("POST", "/reservations/" + id + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(0.0, cancelled.get("refund").asDouble());

        String messages = send("GET", "/messages", null).body();
        assertTrue(messages.contains("Pagamento recebido - reserva " + id));
        assertTrue(messages.contains("Reserva cancelada - quarto 301"));
        assertTrue(!messages.contains("Reembolso - reserva " + id));
    }

    @Test
    void validationErrors() throws Exception {
        assertEquals("INVALID_REQUEST", errorOf(send("POST", "/reservations", "não é json")));
        assertEquals("INVALID_REQUEST", errorOf(send("POST", "/reservations", booking(101, "03-12", "03-10", 1, "STANDARD"))));
        assertEquals("INVALID_REQUEST", errorOf(send("POST", "/reservations", booking(101, "03-10", "03-12", 0, "STANDARD"))));
        assertEquals("ROOM_NOT_FOUND", errorOf(send("POST", "/reservations", booking(999, "03-10", "03-12", 1, "STANDARD"))));
        assertEquals("CAPACITY_EXCEEDED", errorOf(send("POST", "/reservations", booking(101, "03-10", "03-12", 3, "STANDARD"))));
        assertEquals("RESERVATION_NOT_FOUND", errorOf(send("GET", "/reservations/99999999", null)));
        assertEquals("INVALID_REQUEST", errorOf(send("GET", "/reports/revenue?month=2027-13", null)));
        assertEquals(5, json.readTree(send("GET", "/rooms", null).body()).size());
    }

    static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}"""
                .formatted(room, YEAR + "-" + in, YEAR + "-" + out, guests, plan);
    }

    static String errorOf(HttpResponse<String> response) throws Exception {
        return json.readTree(response.body()).get("error").asText();
    }

    static HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
}
