package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotel.Hotel;
import hotel.messages.EmailConfig;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailIntegrationTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Test Guest", "guestEmail": "guest@example.com",
             "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-12",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private Hotel hotel;
    private String baseUrl;

    @BeforeEach
    void startHotel() {
        hotel = new Hotel();
        baseUrl = "http://localhost:" + hotel.start(0);
    }

    @AfterEach
    void stopHotel() {
        hotel.stop();
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
    void confirmationEmailHasStatusField() throws Exception {
        long id = json.readTree(post("/reservations", RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertEquals("guest@example.com", messages.get(0).get("to").asText());
    }

    @Test
    void invalidEmailIsBlocked() throws Exception {
        String invalidJson = RESERVATION_JSON.replace("guest@example.com", "invalid@email");
        long id = json.readTree(post("/reservations", invalidJson).body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
        assertEquals("invalid@email", messages.get(0).get("to").asText());
    }

    @Test
    void confirmationWithValidateMessageStatus() throws Exception {
        String validJson = RESERVATION_JSON.replace("guest@example.com", "test.user@example.co.uk");
        long id = json.readTree(post("/reservations", validJson).body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertEquals("test.user@example.co.uk", messages.get(0).get("to").asText());
    }
}
