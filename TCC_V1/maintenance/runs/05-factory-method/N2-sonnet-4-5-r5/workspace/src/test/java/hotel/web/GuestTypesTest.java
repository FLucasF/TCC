package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hotel.EmptyDatabase;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reservations.PriceCalculator;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuestTypesTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final HttpApi api = new HttpApi(
            new Rooms(database.dataSource()),
            new ReservationService(
                    new Rooms(database.dataSource()),
                    new ReservationStore(database.dataSource()),
                    new PriceCalculator(),
                    new EmailSender(new SentMessages(database.dataSource()))),
            new SentMessages(database.dataSource()),
            null,
            null);

    private final ObjectMapper json = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final HttpClient http = HttpClient.newHttpClient();
    private String baseUrl;

    @BeforeEach
    void startServer() {
        api.server().start(0);
        baseUrl = "http://localhost:" + api.server().port();
    }

    @AfterEach
    void stopServer() {
        api.server().stop();
        database.close();
    }

    @Test
    void individualGuestWithCpf() throws Exception {
        String requestBody = """
                {
                  "roomNumber": 201,
                  "guestName": "Carla Dias",
                  "guestEmail": "carla@example.com",
                  "guestCpf": "111.222.333-44",
                  "checkIn": "2027-07-10",
                  "checkOut": "2027-07-12",
                  "guests": 2,
                  "ratePlan": "STANDARD"
                }
                """;

        HttpResponse<String> response = post("/reservations", requestBody);
        assertEquals(201, response.statusCode());

        JsonNode reservation = json.readTree(response.body());
        assertEquals("111.222.333-44", reservation.get("guestCpf").asText());
        assertTrue(reservation.get("companyCnpj").isNull());
        assertTrue(reservation.get("guestPassport").isNull());

        long id = reservation.get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");
        post("/reservations/" + id + "/check-in", "");

        HttpResponse<String> checkedOut = post("/reservations/" + id + "/check-out", "");
        JsonNode document = json.readTree(checkedOut.body());
        assertTrue(document.get("text").asText().startsWith("Comprovante de reserva"));
        assertTrue(document.get("text").asText().contains("CPF 111.222.333-44"));
        assertEquals("111.222.333-44", document.get("guestCpf").asText());
        assertTrue(document.get("iss").isNull());
    }

    @Test
    void companyGuestWithCnpj() throws Exception {
        String requestBody = """
                {
                  "roomNumber": 201,
                  "guestName": "Ana Souza",
                  "guestEmail": "ana@example.com",
                  "companyCnpj": "12.345.678/0001-90",
                  "companyName": "Tecnosul Ltda",
                  "checkIn": "2027-03-10",
                  "checkOut": "2027-03-12",
                  "guests": 1,
                  "ratePlan": "STANDARD"
                }
                """;

        HttpResponse<String> response = post("/reservations", requestBody);
        assertEquals(201, response.statusCode());

        JsonNode reservation = json.readTree(response.body());
        assertTrue(reservation.get("guestCpf").isNull());
        assertEquals("12.345.678/0001-90", reservation.get("companyCnpj").asText());
        assertEquals("Tecnosul Ltda", reservation.get("companyName").asText());
        assertTrue(reservation.get("guestPassport").isNull());

        long id = reservation.get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");
        post("/reservations/" + id + "/check-in", "");

        HttpResponse<String> checkedOut = post("/reservations/" + id + "/check-out", "");
        JsonNode document = json.readTree(checkedOut.body());
        assertTrue(document.get("text").asText().startsWith("Fatura de hospedagem"));
        assertTrue(document.get("text").asText().contains("CNPJ 12.345.678/0001-90"));
        assertTrue(document.get("text").asText().contains("Empresa: Tecnosul Ltda"));
        assertTrue(document.get("text").asText().contains("ISS retido (5%)"));
        assertEquals("12.345.678/0001-90", document.get("companyCnpj").asText());
        assertEquals("Tecnosul Ltda", document.get("companyName").asText());
        assertEquals("38.99", document.get("iss").asText());
    }

    @Test
    void foreignGuestWithPassport() throws Exception {
        String requestBody = """
                {
                  "roomNumber": 201,
                  "guestName": "John Smith",
                  "guestEmail": "john@example.com",
                  "guestPassport": "X1234567",
                  "guestCountry": "United Kingdom",
                  "checkIn": "2027-03-10",
                  "checkOut": "2027-03-12",
                  "guests": 1,
                  "ratePlan": "STANDARD"
                }
                """;

        HttpResponse<String> response = post("/reservations", requestBody);
        assertEquals(201, response.statusCode());

        JsonNode reservation = json.readTree(response.body());
        assertTrue(reservation.get("guestCpf").isNull());
        assertTrue(reservation.get("companyCnpj").isNull());
        assertEquals("X1234567", reservation.get("guestPassport").asText());
        assertEquals("United Kingdom", reservation.get("guestCountry").asText());

        long id = reservation.get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");
        post("/reservations/" + id + "/check-in", "");

        HttpResponse<String> checkedOut = post("/reservations/" + id + "/check-out", "");
        JsonNode document = json.readTree(checkedOut.body());
        assertTrue(document.get("text").asText().startsWith("Booking receipt"));
        assertTrue(document.get("text").asText().contains("passport X1234567"));
        assertTrue(document.get("text").asText().contains("United Kingdom"));
        assertTrue(document.get("text").asText().contains("2027-03-10"));
        assertTrue(document.get("text").asText().contains("BRL 779.80"));
        assertEquals("X1234567", document.get("guestPassport").asText());
        assertEquals("United Kingdom", document.get("guestCountry").asText());
        assertTrue(document.get("iss").isNull());
    }

    @Test
    void rejectsReservationWithNoIdentification() throws Exception {
        String requestBody = """
                {
                  "roomNumber": 201,
                  "guestName": "Maria Silva",
                  "guestEmail": "maria@example.com",
                  "checkIn": "2027-03-10",
                  "checkOut": "2027-03-12",
                  "guests": 1,
                  "ratePlan": "STANDARD"
                }
                """;

        HttpResponse<String> response = post("/reservations", requestBody);
        assertEquals(400, response.statusCode());
        JsonNode error = json.readTree(response.body());
        assertEquals("INVALID_REQUEST", error.get("error").asText());
        assertTrue(error.get("message").asText().contains("exactly one identification"));
    }

    @Test
    void rejectsReservationWithMultipleIdentifications() throws Exception {
        String requestBody = """
                {
                  "roomNumber": 201,
                  "guestName": "Pedro Santos",
                  "guestEmail": "pedro@example.com",
                  "guestCpf": "111.222.333-44",
                  "companyCnpj": "12.345.678/0001-90",
                  "companyName": "Empresa Teste",
                  "checkIn": "2027-03-10",
                  "checkOut": "2027-03-12",
                  "guests": 1,
                  "ratePlan": "STANDARD"
                }
                """;

        HttpResponse<String> response = post("/reservations", requestBody);
        assertEquals(400, response.statusCode());
        JsonNode error = json.readTree(response.body());
        assertEquals("INVALID_REQUEST", error.get("error").asText());
        assertTrue(error.get("message").asText().contains("exactly one identification"));
    }

    @Test
    void rejectsCompanyWithoutCompanyName() throws Exception {
        String requestBody = """
                {
                  "roomNumber": 201,
                  "guestName": "Ana Souza",
                  "guestEmail": "ana@example.com",
                  "companyCnpj": "12.345.678/0001-90",
                  "checkIn": "2027-03-10",
                  "checkOut": "2027-03-12",
                  "guests": 1,
                  "ratePlan": "STANDARD"
                }
                """;

        HttpResponse<String> response = post("/reservations", requestBody);
        assertEquals(400, response.statusCode());
    }

    @Test
    void rejectsForeignWithoutCountry() throws Exception {
        String requestBody = """
                {
                  "roomNumber": 201,
                  "guestName": "John Smith",
                  "guestEmail": "john@example.com",
                  "guestPassport": "X1234567",
                  "checkIn": "2027-03-10",
                  "checkOut": "2027-03-12",
                  "guests": 1,
                  "ratePlan": "STANDARD"
                }
                """;

        HttpResponse<String> response = post("/reservations", requestBody);
        assertEquals(400, response.statusCode());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        return http.send(request, BodyHandlers.ofString());
    }
}
