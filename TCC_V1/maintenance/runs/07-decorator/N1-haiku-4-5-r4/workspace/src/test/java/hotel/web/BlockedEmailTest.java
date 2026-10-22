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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockedEmailTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "invalid@email",
             "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-12",
             "guests": 2, "ratePlan": "STANDARD"}
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
    void blockedEmailAppearInMessagesList() throws Exception {
        String validJson = RESERVATION_JSON
                .replace("invalid@email", "valid@email.com")
                .replace("201", "202");

        HttpResponse<String> invalidCreated = post("/reservations", RESERVATION_JSON);
        assertEquals(201, invalidCreated.statusCode());
        JsonNode invalidReservation = json.readTree(invalidCreated.body());
        long invalidId = invalidReservation.get("id").asLong();

        HttpResponse<String> validCreated = post("/reservations", validJson);
        assertEquals(201, validCreated.statusCode());
        JsonNode validReservation = json.readTree(validCreated.body());
        long validId = validReservation.get("id").asLong();

        post("/reservations/" + invalidId + "/confirmation", "");
        post("/reservations/" + validId + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(3, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
        assertEquals("invalid@email", messages.get(0).get("to").asText());
        assertEquals("SENT", messages.get(1).get("status").asText());
        assertEquals("valid@email.com", messages.get(1).get("to").asText());
        assertEquals("SENT", messages.get(2).get("status").asText());
        assertEquals("arquivo@pousadamarealta.com.br", messages.get(2).get("to").asText());
    }
}
