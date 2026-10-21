package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotel.Hotel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailRulesTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "%s",
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

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void invalidEmailIsBlocked() throws Exception {
        String reservationWithInvalidEmail = RESERVATION_JSON.formatted("carla@nomundodot");
        long id = json.readTree(post("/reservations", reservationWithInvalidEmail).body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
        assertEquals("carla@nomundodot", messages.get(0).get("to").asText());
    }

    @Test
    void validEmailCreatesOriginalAndArchiveCopy() throws Exception {
        String reservationWithValidEmail = RESERVATION_JSON.formatted("carla@example.com");
        long id = json.readTree(post("/reservations", reservationWithValidEmail).body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(2, messages.size());

        JsonNode original = messages.get(0);
        assertEquals("SENT", original.get("status").asText());
        assertEquals("carla@example.com", original.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", original.get("subject").asText());

        JsonNode archiveCopy = messages.get(1);
        assertEquals("SENT", archiveCopy.get("status").asText());
        assertEquals("arquivo@pousadamarealta.com.br", archiveCopy.get("to").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", archiveCopy.get("subject").asText());
    }

    @Test
    void invalidEmailIsBlockedAndHasNoArchiveCopy() throws Exception {
        String reservationWithInvalidEmail = RESERVATION_JSON.formatted("carla@invalid");
        long id = json.readTree(post("/reservations", reservationWithInvalidEmail).body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
    }
}
