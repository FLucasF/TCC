package br.com.marealta.pousada;

import br.com.marealta.pousada.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/** Roda contra o PostgreSQL de DB_URL, num schema próprio que é apagado no fim. Sem DB_URL, é ignorado. */
class EndToEndTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static String baseUrl;
    private static String adminUrl;
    private static Javalin app;

    @BeforeEach
    void start() throws Exception {
        adminUrl = System.getenv("DB_URL");
        assumeTrue(adminUrl != null && !adminUrl.isBlank(), "DB_URL ausente");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        String sep = adminUrl.contains("?") ? "&" : "?";
        app = App.start(new Database(adminUrl + sep + "currentSchema=" + schema,
                System.getenv("DB_USER"), System.getenv("DB_PASSWORD")), 0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() throws Exception {
        if (app != null) app.stop();
        if (schema != null) {
            try (Connection c = admin(); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static Connection admin() throws Exception {
        return DriverManager.getConnection(adminUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String reservation(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        JsonNode rooms = JSON.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(389.90, rooms.get(2).get("nightlyRate").asDouble());

        HttpResponse<String> created = call("POST", "/reservations", reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        JsonNode r = JSON.readTree(created.body());
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(created.body().contains("\"subtotal\":779.80"));
        assertTrue(r.get("refund").isNull());

        // conflito de noites e demais erros
        assertEquals("ROOM_UNAVAILABLE", JSON.readTree(call("POST", "/reservations",
                reservation(201, "2027-03-11", "2027-03-13", 1, "STANDARD")).body()).get("error").asText());
        assertEquals(409, call("POST", "/reservations", reservation(201, "2027-03-11", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", reservation(201, "2027-03-12", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", reservation(101, "2027-05-01", "2027-05-02", 3, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", reservation(999, "2027-05-01", "2027-05-02", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-05-02", "2027-05-02", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-05-01", "2027-05-02", 0, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "{nao e json").statusCode());
        assertEquals(400, call("POST", "/reservations", "{}").statusCode());
        assertEquals(404, call("GET", "/reservations/9999", null).statusCode());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).statusCode());

        // confirmação e mensagens
        JsonNode confirmed = JSON.readTree(call("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals(779.80, confirmed.get("amountPaid").asDouble());
        JsonNode messages = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(3, messages.size());
        boolean guestMail = false, house = false, finance = false;
        for (JsonNode m : messages) {
            assertEquals("EMAIL", m.get("channel").asText());
            switch (m.get("to").asText()) {
                case "carla@example.com" -> {
                    guestMail = true;
                    assertEquals("Reserva confirmada - Pousada Maré Alta", m.get("subject").asText());
                    assertTrue(m.get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta"));
                    assertTrue(m.get("body").asText().endsWith("Total: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"));
                }
                case "governanca@pousadamarealta.com.br" -> {
                    house = true;
                    assertEquals("Reserva confirmada - quarto 201", m.get("subject").asText());
                    assertTrue(m.get("body").asText().contains("10/03/2027"));
                    assertTrue(m.get("body").asText().contains("12/03/2027"));
                }
                case "financeiro@pousadamarealta.com.br" -> {
                    finance = true;
                    assertEquals("Pagamento recebido - reserva " + id, m.get("subject").asText());
                    assertTrue(m.get("body").asText().contains("R$ 779,80"));
                }
                default -> fail("destinatário inesperado");
            }
        }
        assertTrue(guestMail && house && finance);
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).statusCode());

        // relatórios
        String occupancy = call("GET", "/reports/occupancy?date=2027-03-11", null).body();
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2027-03-10,2027-03-12\ntotal,1/5\n", occupancy);
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));
        assertEquals(400, call("GET", "/reports/occupancy?date=11-03-2027", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).statusCode());
        HttpResponse<String> revenue = call("GET", "/reports/revenue?month=2027-03", null);
        assertEquals("text/csv; charset=utf-8", revenue.headers().firstValue("Content-Type").orElse(""));
        assertEquals("reservation,room,check_in,check_out,status,amount\n" + id + ",201,2027-03-10,2027-03-12,CONFIRMED,779.80\ntotal,779.80\n", revenue.body());

        // check-in, check-out, segunda via
        assertEquals("CHECKED_IN", JSON.readTree(call("POST", "/reservations/" + id + "/check-in", null).body()).get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).statusCode());
        JsonNode receipt = JSON.readTree(call("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals(id, receipt.get("reservationId").asLong());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals(receipt, JSON.readTree(call("GET", "/reservations/" + id + "/document", null).body()));
    }

    @Test
    void cancellationNotifications() throws Exception {
        int before = JSON.readTree(call("GET", "/messages", null).body()).size();

        // pendente cancelada: ninguém é avisado
        long pending = JSON.readTree(call("POST", "/reservations", reservation(301, "2027-08-01", "2027-08-03", 4, "STANDARD")).body()).get("id").asLong();
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/" + pending + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(0.0, cancelled.get("refund").asDouble());
        assertEquals(before, JSON.readTree(call("GET", "/messages", null).body()).size());
        // quarto liberado
        assertEquals(201, call("POST", "/reservations", reservation(301, "2027-08-01", "2027-08-03", 4, "STANDARD")).statusCode());

        // confirmada, plano normal: governança e financeiro
        long refundable = JSON.readTree(call("POST", "/reservations", reservation(102, "2027-08-01", "2027-08-03", 2, "STANDARD")).body()).get("id").asLong();
        call("POST", "/reservations/" + refundable + "/confirmation", null);
        int mid = JSON.readTree(call("GET", "/messages", null).body()).size();
        call("POST", "/reservations/" + refundable + "/cancellation", null);
        JsonNode msgs = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(mid + 2, msgs.size());
        StringBuilder subjects = new StringBuilder();
        for (int i = mid; i < msgs.size(); i++) subjects.append(msgs.get(i).get("subject").asText()).append('|');
        assertTrue(subjects.toString().contains("Reserva cancelada - quarto 102"));
        assertTrue(subjects.toString().contains("Reembolso - reserva " + refundable));

        // confirmada, não reembolsável: só a governança
        long locked = JSON.readTree(call("POST", "/reservations", reservation(101, "2027-08-01", "2027-08-03", 2, "NON_REFUNDABLE")).body()).get("id").asLong();
        call("POST", "/reservations/" + locked + "/confirmation", null);
        int mid2 = JSON.readTree(call("GET", "/messages", null).body()).size();
        JsonNode lockedCancelled = JSON.readTree(call("POST", "/reservations/" + locked + "/cancellation", null).body());
        assertEquals(0.0, lockedCancelled.get("refund").asDouble());
        assertEquals(mid2 + 1, JSON.readTree(call("GET", "/messages", null).body()).size());
        // o dinheiro retido continua no financeiro
        String revenue = call("GET", "/reports/revenue?month=2027-08", null).body();
        assertTrue(revenue.contains(locked + ",101,2027-08-01,2027-08-03,CANCELLED,"));
        assertFalse(revenue.contains("," + refundable + ","));
    }
}
