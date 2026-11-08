package br.com.marealta;

import br.com.marealta.web.App;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpApiTest {
    private static Javalin server;
    private static String base;
    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    static void start() {
        InMemory m = new InMemory();
        server = new App(m.service, m.reports).create().start(0);
        base = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final String VALID = """
            {"roomNumber":201,"guestName":"Carla Dias","guestEmail":"c@x.com","guestCpf":"11122233344",
             "checkIn":"2027-03-10","checkOut":"2027-03-12","guests":2,"ratePlan":"STANDARD"}""";

    @Test
    void fullFlow() throws Exception {
        JsonNode rooms = json.readTree(send("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(389.90, rooms.get(2).get("nightlyRate").asDouble());

        HttpResponse<String> created = send("POST", "/reservations", VALID);
        assertEquals(201, created.statusCode());
        JsonNode r = json.readTree(created.body());
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("2027-03-10", r.get("checkIn").asText());
        assertTrue(r.get("refund").isNull());
        assertTrue(created.body().contains("\"amountPaid\":0.00"), created.body());
        assertTrue(created.body().contains("\"total\":779.80"), created.body());

        assertEquals(409, send("POST", "/reservations", VALID).statusCode());
        assertEquals(409, send("GET", "/reservations/" + id + "/document", null).statusCode());
        assertEquals("CONFIRMED", json.readTree(send("POST", "/reservations/" + id + "/confirmation", null).body()).get("status").asText());
        assertEquals("CHECKED_IN", json.readTree(send("POST", "/reservations/" + id + "/check-in", null).body()).get("status").asText());

        HttpResponse<String> out = send("POST", "/reservations/" + id + "/check-out", null);
        assertEquals(200, out.statusCode());
        JsonNode doc = json.readTree(out.body());
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("DELUXE", doc.get("roomType").asText());
        assertEquals(json.readTree(send("GET", "/reservations/" + id + "/document", null).body()), doc);

        JsonNode msgs = json.readTree(send("GET", "/messages", null).body());
        assertEquals(3, msgs.size());
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());

        HttpResponse<String> csv = send("GET", "/reports/revenue?month=2027-03", null);
        assertTrue(csv.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertTrue(csv.headers().firstValue("content-type").orElse("").toLowerCase().contains("utf-8"));
        assertTrue(csv.body().endsWith("total,779.80\n"));
        assertTrue(send("GET", "/reports/occupancy?date=2027-03-10", null).body().endsWith("total,1/5\n"));
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, send("POST", "/reservations", "não é json").statusCode());
        assertEquals(400, send("POST", "/reservations", "{}").statusCode());
        assertEquals(400, send("POST", "/reservations", VALID.replace("\"guests\":2", "\"guests\":\"2\"")).statusCode());
        assertEquals(400, send("POST", "/reservations", VALID.replace("2027-03-12", "12/03/2027")).statusCode());
        assertEquals(400, send("POST", "/reservations", VALID.replace("2027-03-12", "2027-03-10")).statusCode());
        assertEquals(400, send("POST", "/reservations", VALID.replace("\"guests\":2", "\"guests\":0")).statusCode());
        assertEquals(400, send("POST", "/reservations", VALID.replace("STANDARD\"", "OUTRO\"")).statusCode());

        HttpResponse<String> noRoom = send("POST", "/reservations", VALID.replace("201", "999"));
        assertEquals(404, noRoom.statusCode());
        assertEquals("ROOM_NOT_FOUND", json.readTree(noRoom.body()).get("error").asText());
        assertEquals(422, send("POST", "/reservations", VALID.replace("\"guests\":2", "\"guests\":9")).statusCode());
        assertEquals(404, send("GET", "/reservations/abc", null).statusCode());
        assertEquals(404, send("POST", "/reservations/12345/confirmation", null).statusCode());
        assertEquals(400, send("GET", "/reports/occupancy?date=10-07-2027", null).statusCode());
        assertEquals(400, send("GET", "/reports/revenue", null).statusCode());
        assertEquals(400, send("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }
}
