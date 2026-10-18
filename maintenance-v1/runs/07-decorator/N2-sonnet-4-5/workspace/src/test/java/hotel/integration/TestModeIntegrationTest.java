package hotel.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotel.Hotel;
import hotel.messages.ArchiveCopyRule;
import hotel.messages.EmailRule;
import hotel.messages.EmailSender;
import hotel.messages.EmailValidationRule;
import hotel.messages.SentMessages;
import hotel.messages.TestModeRedirectionRule;
import hotel.reports.OccupancyReport;
import hotel.reports.RevenueReport;
import hotel.reservations.PriceCalculator;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import hotel.web.HttpApi;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestModeIntegrationTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
             "guestCpf": "111.222.333-44", "checkIn": "2027-07-10", "checkOut": "2027-07-12",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private Javalin server;
    private String baseUrl;

    @BeforeEach
    void startServer() {
        Rooms rooms = new Rooms();
        ReservationStore store = new ReservationStore();
        SentMessages sentMessages = new SentMessages();

        List<EmailRule> emailRules = List.of(
                new EmailValidationRule(),
                new TestModeRedirectionRule(true),
                new ArchiveCopyRule(true)
        );

        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages, emailRules));
        HttpApi api = new HttpApi(
                rooms, reservations, sentMessages, new OccupancyReport(rooms, store), new RevenueReport(store));
        server = api.server();
        int port = server.start(0).port();
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    void stopServer() {
        server.stop();
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
    void redirectsEmailsToTestAddressInTestMode() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());

        JsonNode message = messages.get(0);
        assertEquals("testes@pousadamarealta.com.br", message.get("to").asText());
        assertEquals("[TESTE] Reserva confirmada - Pousada Maré Alta", message.get("subject").asText());
        assertEquals("SENT", message.get("status").asText());
        assertEquals(true, message.get("body").asText().startsWith("Destinatário original: carla@example.com\n"));
    }

    @Test
    void blocksInvalidEmailsEvenInTestMode() throws Exception {
        String invalidEmail = RESERVATION_JSON.replace("carla@example.com", "carla@invalid");
        HttpResponse<String> created = post("/reservations", invalidEmail);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());

        JsonNode message = messages.get(0);
        assertEquals("carla@invalid", message.get("to").asText());
        assertEquals("BLOCKED", message.get("status").asText());
    }
}
