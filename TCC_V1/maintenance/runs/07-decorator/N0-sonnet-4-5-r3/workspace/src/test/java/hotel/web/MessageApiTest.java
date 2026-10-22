package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotel.EmptyDatabase;
import hotel.Hotel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MessageApiTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
             "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-12",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

    private static final String INVALID_EMAIL_RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Ana Silva", "guestEmail": "ana@gmail",
             "guestCpf": "222.333.444-55", "checkIn": "2027-07-15", "checkOut": "2027-07-17",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final EmptyDatabase database = new EmptyDatabase();
    private Hotel hotel;
    private String baseUrl;

    @AfterEach
    void stopHotel() {
        if (hotel != null) {
            hotel.stop();
        }
        database.close();
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void normalModeCreatesArchiveCopy() throws Exception {
        hotel = new Hotel(database.dataSource(), false);
        baseUrl = "http://localhost:" + hotel.start(0);

        long id = json.readTree(post("/reservations", RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(2, messages.size());

        assertEquals("carla@example.com", messages.get(0).get("to").asText());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).get("to").asText());
        assertEquals("SENT", messages.get(1).get("status").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).get("subject").asText());
    }

    @Test
    void testModeRedirectsToTestAddress() throws Exception {
        hotel = new Hotel(database.dataSource(), true);
        baseUrl = "http://localhost:" + hotel.start(0);

        long id = json.readTree(post("/reservations", RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());

        assertEquals("testes@pousadamarealta.com.br", messages.get(0).get("to").asText());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertEquals("[TESTE] Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
        String body = messages.get(0).get("body").asText();
        assertEquals(true, body.startsWith("Destinatário original: carla@example.com\n"));
    }

    @Test
    void invalidEmailIsBlocked() throws Exception {
        hotel = new Hotel(database.dataSource(), false);
        baseUrl = "http://localhost:" + hotel.start(0);

        long id = json.readTree(post("/reservations", INVALID_EMAIL_RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());

        assertEquals("ana@gmail", messages.get(0).get("to").asText());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
    }

    @Test
    void invalidEmailIsBlockedInTestMode() throws Exception {
        hotel = new Hotel(database.dataSource(), true);
        baseUrl = "http://localhost:" + hotel.start(0);

        long id = json.readTree(post("/reservations", INVALID_EMAIL_RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());

        assertEquals("ana@gmail", messages.get(0).get("to").asText());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
    }
}
