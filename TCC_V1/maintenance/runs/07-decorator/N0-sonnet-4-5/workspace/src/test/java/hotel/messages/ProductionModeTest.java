package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotel.EmptyDatabase;
import hotel.Hotel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductionModeTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Maria Silva", "guestEmail": "maria@example.com",
             "guestCpf": "123.456.789-00", "checkIn": "2027-08-10", "checkOut": "2027-08-12",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final EmptyDatabase database = new EmptyDatabase();
    private Hotel hotel;
    private String baseUrl;

    @BeforeEach
    void startHotel() {
        hotel = new Hotel(database.dataSource(), false);
        baseUrl = "http://localhost:" + hotel.start(0);
    }

    @AfterEach
    void stopHotel() {
        hotel.stop();
        database.close();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void sendsEmailWithArchiveCopyInProductionMode() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        long id = json.readTree(created.body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(2, messages.size());

        JsonNode original = messages.get(0);
        assertEquals("maria@example.com", original.get("to").asText());
        assertEquals("SENT", original.get("status").asText());
        assertTrue(original.get("subject").asText().contains("Reserva confirmada"));

        JsonNode copy = messages.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", copy.get("to").asText());
        assertEquals("SENT", copy.get("status").asText());
        assertTrue(copy.get("subject").asText().startsWith("[Cópia] "));
        assertTrue(copy.get("subject").asText().contains("Reserva confirmada"));
        assertEquals(original.get("body").asText(), copy.get("body").asText());
    }

    @Test
    void blocksInvalidEmailInProductionMode() throws Exception {
        String invalidEmailJson = """
            {"roomNumber": 201, "guestName": "Test", "guestEmail": "invalid@email",
             "guestCpf": "123.456.789-00", "checkIn": "2027-09-10", "checkOut": "2027-09-12",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> created = post("/reservations", invalidEmailJson);
        long id = json.readTree(created.body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());

        JsonNode blocked = messages.get(0);
        assertEquals("invalid@email", blocked.get("to").asText());
        assertEquals("BLOCKED", blocked.get("status").asText());
    }
}
