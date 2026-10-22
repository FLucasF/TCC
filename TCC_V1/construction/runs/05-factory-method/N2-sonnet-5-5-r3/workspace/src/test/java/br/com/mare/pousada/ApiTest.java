package br.com.mare.pousada;

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
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = Main.start(new InMemoryRepositories.Reservations(), new InMemoryRepositories.Messages(), 0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher pub = body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).method(method, pub).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String booking(String identity, int room, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"c@d.com\"," + identity
                + ",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out + "\",\"guests\":" + guests
                + ",\"ratePlan\":\"" + plan + "\"}";
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void fullFlowWithMessagesAndReports() throws Exception {
        HttpResponse<String> created = call("POST", "/reservations",
                booking(CPF, 201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        JsonNode r = json.readTree(created.body());
        assertEquals(1, r.get("id").asInt());
        assertTrue(r.get("companyCnpj").isNull());
        assertTrue(r.get("refund").isNull());
        assertEquals("PENDING", r.get("status").asText());

        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());

        JsonNode msgs = json.readTree(call("GET", "/messages", null).body());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertTrue(msgs.get(0).get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva"));

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertTrue(occ.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        JsonNode doc = json.readTree(call("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals(779.80, doc.get("total").asDouble());
        assertTrue(doc.get("text").asText().endsWith("Total: R$ 779,80"));
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());

        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void identityRules() throws Exception {
        String dates = "2027-03-10";
        assertEquals(400, call("POST", "/reservations", booking("\"companyCnpj\":\"1\"", 101, dates, "2027-03-11", 1,
                "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(CPF + ",\"guestPassport\":\"A\",\"guestCountry\":\"B\"",
                101, dates, "2027-03-11", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking("\"x\":1", 101, dates, "2027-03-11", 1,
                "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", booking("\"guestPassport\":\"A\",\"guestCountry\":\"UK\"", 101,
                dates, "2027-03-11", 1, "NON_REFUNDABLE")).statusCode());
        JsonNode r = json.readTree(call("GET", "/reservations/1", null).body());
        assertEquals("UK", r.get("guestCountry").asText());
        assertTrue(r.get("guestCpf").isNull());
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao json").statusCode());
        assertEquals(400, call("POST", "/reservations",
                booking(CPF, 101, "2027-03-10", "2027-03-10", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations",
                booking(CPF, 101, "2027-03-10", "2027-03-11", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations",
                booking(CPF, 999, "2027-03-10", "2027-03-11", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations",
                booking(CPF, 101, "2027-03-10", "2027-03-11", 3, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations",
                booking(CPF, 101, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        HttpResponse<String> clash = call("POST", "/reservations",
                booking(CPF, 101, "2027-03-11", "2027-03-13", 1, "STANDARD"));
        assertEquals(409, clash.statusCode());
        assertEquals("ROOM_UNAVAILABLE", json.readTree(clash.body()).get("error").asText());
        assertEquals(201, call("POST", "/reservations",
                booking(CPF, 101, "2027-03-12", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/cancellation", null).statusCode());
        assertEquals(0.0, json.readTree(call("GET", "/reservations/1", null).body()).get("refund").asDouble());
        assertEquals(5, json.readTree(call("GET", "/rooms", null).body()).size());
    }
}
