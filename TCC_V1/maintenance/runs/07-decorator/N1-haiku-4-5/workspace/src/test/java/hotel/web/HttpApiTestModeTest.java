package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotel.EmptyDatabase;
import hotel.Hotel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpApiTestModeTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
             "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-12",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final EmptyDatabase database = new EmptyDatabase();
    private Hotel hotel;
    private String baseUrl;

    @BeforeEach
    void startHotelInTestMode() {
        hotel = new Hotel(database.dataSource(), Map.of("HOTEL_TEST_MODE", "true"));
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
    void testModeRedirectsEmailsToTestAddress() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");
        JsonNode messages = json.readTree(get("/messages").body());

        assertEquals(1, messages.size());
        JsonNode testMessage = messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", testMessage.get("to").asText());
        assertEquals("SENT", testMessage.get("status").asText());
        assertTrue(testMessage.get("subject").asText().startsWith("[TESTE] "));
        assertTrue(testMessage.get("body").asText().startsWith("Destinatário original: carla@example.com"));
    }

    @Test
    void testModeBlocksInvalidEmailWithOriginalAddress() throws Exception {
        String invalidEmailJson = """
                {"roomNumber": 201, "guestName": "Ana Souza", "guestEmail": "ana@invalid",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-12",
                 "guests": 2, "ratePlan": "STANDARD"}
                """;

        HttpResponse<String> created = post("/reservations", invalidEmailJson);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");
        JsonNode messages = json.readTree(get("/messages").body());

        assertEquals(1, messages.size());
        JsonNode blockedMessage = messages.get(0);
        assertEquals("ana@invalid", blockedMessage.get("to").asText());
        assertEquals("BLOCKED", blockedMessage.get("status").asText());
    }
}
