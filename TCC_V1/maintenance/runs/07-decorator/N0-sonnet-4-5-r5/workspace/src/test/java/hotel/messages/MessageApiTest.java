package hotel.messages;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessageApiTest {

    private static final String VALID_RESERVATION_JSON = """
            {"roomNumber": 101, "guestName": "Test Guest", "guestEmail": "valid@example.com",
             "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

    private static final String INVALID_EMAIL_RESERVATION_JSON = """
            {"roomNumber": 102, "guestName": "Test Guest", "guestEmail": "invalid.email",
             "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final EmptyDatabase database = new EmptyDatabase();
    private Hotel hotel;
    private String baseUrl;

    @BeforeEach
    void startHotel() {
        hotel = new Hotel(database.dataSource());
        baseUrl = "http://localhost:" + hotel.start(0);
    }

    @AfterEach
    void stopHotel() {
        hotel.stop();
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
    void sentMessagesHaveStatusSentAndArchiveCopy() throws Exception {
        long id = json.readTree(post("/reservations", VALID_RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(2, messages.size());

        assertEquals("valid@example.com", messages.get(0).get("to").asText());
        assertEquals("SENT", messages.get(0).get("status").asText());

        assertEquals("arquivo@pousadamarealta.com.br", messages.get(1).get("to").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).get("subject").asText());
        assertEquals("SENT", messages.get(1).get("status").asText());
    }

    @Test
    void blockedMessagesHaveStatusBlocked() throws Exception {
        long id = json.readTree(post("/reservations", INVALID_EMAIL_RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("invalid.email", messages.get(0).get("to").asText());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
    }
}
