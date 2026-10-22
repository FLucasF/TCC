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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpApiTest {

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
    void listsTheRooms() throws Exception {
        HttpResponse<String> response = get("/rooms");

        assertEquals(200, response.statusCode());
        JsonNode rooms = json.readTree(response.body());
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertEquals("249.9", rooms.get(0).get("nightlyRate").asText());
    }

    @Test
    void booksConfirmsAndChecksOut() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        assertEquals(201, created.statusCode());
        JsonNode reservation = json.readTree(created.body());
        long id = reservation.get("id").asLong();
        assertEquals("PENDING", reservation.get("status").asText());
        assertEquals("2027-07-10", reservation.get("checkIn").asText());
        assertEquals(935.76, reservation.get("total").asDouble());

        assertEquals(200, post("/reservations/" + id + "/confirmation", "").statusCode());
        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("carla@example.com", messages.get(0).get("to").asText());

        assertEquals(200, post("/reservations/" + id + "/check-in", "").statusCode());
        HttpResponse<String> checkedOut = post("/reservations/" + id + "/check-out", "");
        assertEquals(200, checkedOut.statusCode());
        assertEquals("Comprovante de reserva", json.readTree(checkedOut.body()).get("title").asText());
        assertEquals("CHECKED_OUT", json.readTree(get("/reservations/" + id).body()).get("status").asText());
    }

    @Test
    void answersErrorsWithACode() throws Exception {
        HttpResponse<String> missingFields = post("/reservations", "{\"roomNumber\": 201}");
        assertEquals(400, missingFields.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(missingFields.body()).get("error").asText());

        assertEquals(400, post("/reservations", "not json").statusCode());
        assertEquals(400, post("/reservations", RESERVATION_JSON.replace("STANDARD", "VIP")).statusCode());
        assertEquals(404, get("/reservations/42").statusCode());
        assertEquals(404, get("/reservations/abc").statusCode());

        post("/reservations", RESERVATION_JSON);
        HttpResponse<String> taken = post("/reservations", RESERVATION_JSON);
        assertEquals(409, taken.statusCode());
        assertEquals("ROOM_UNAVAILABLE", json.readTree(taken.body()).get("error").asText());
    }

    @Test
    void servesTheReportsAsCsv() throws Exception {
        long id = json.readTree(post("/reservations", RESERVATION_JSON).body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        HttpResponse<String> occupancy = get("/reports/occupancy?date=2027-07-11");
        assertEquals(200, occupancy.statusCode());
        assertTrue(occupancy.headers().firstValue("Content-Type").orElse("").startsWith("text/csv"));
        assertTrue(occupancy.body().endsWith("total,1/5\n"));

        assertTrue(get("/reports/revenue?month=2027-07").body().endsWith("total,935.76\n"));
        assertEquals(400, get("/reports/revenue?month=july").statusCode());
        assertEquals(400, get("/reports/occupancy").statusCode());
    }

    @Test
    void booksForACompanyAndIssuesInvoice() throws Exception {
        String companyReservation = """
                {"roomNumber": 201, "guestName": "Ana Souza", "guestEmail": "ana@example.com",
                 "companyCnpj": "12.345.678/0001-90", "companyName": "Tecnosul Ltda",
                 "checkIn": "2027-03-10", "checkOut": "2027-03-12", "guests": 2, "ratePlan": "STANDARD"}
                """;
        HttpResponse<String> created = post("/reservations", companyReservation);
        assertEquals(201, created.statusCode());
        JsonNode reservation = json.readTree(created.body());
        long id = reservation.get("id").asLong();
        assertEquals("12.345.678/0001-90", reservation.get("companyCnpj").asText());
        assertEquals("Tecnosul Ltda", reservation.get("companyName").asText());
        assertTrue(reservation.get("guestCpf").isNull());

        post("/reservations/" + id + "/confirmation", "");
        post("/reservations/" + id + "/check-in", "");
        HttpResponse<String> checkedOut = post("/reservations/" + id + "/check-out", "");
        JsonNode document = json.readTree(checkedOut.body());
        assertEquals("Fatura de hospedagem", document.get("title").asText());
        assertTrue(document.get("text").asText().contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(document.get("text").asText().contains("ISS retido (5%):"));
    }

    @Test
    void booksForAForeignGuestAndIssuesReceiptInEnglish() throws Exception {
        String foreignReservation = """
                {"roomNumber": 201, "guestName": "John Smith", "guestEmail": "john@example.com",
                 "guestPassport": "X1234567", "guestCountry": "United Kingdom",
                 "checkIn": "2027-03-10", "checkOut": "2027-03-12", "guests": 2, "ratePlan": "STANDARD"}
                """;
        HttpResponse<String> created = post("/reservations", foreignReservation);
        assertEquals(201, created.statusCode());
        JsonNode reservation = json.readTree(created.body());
        long id = reservation.get("id").asLong();
        assertEquals("X1234567", reservation.get("guestPassport").asText());
        assertEquals("United Kingdom", reservation.get("guestCountry").asText());
        assertTrue(reservation.get("guestCpf").isNull());

        post("/reservations/" + id + "/confirmation", "");
        HttpResponse<String> documentResponse = get("/reservations/" + id + "/document");
        JsonNode document = json.readTree(documentResponse.body());
        assertEquals("Booking receipt", document.get("title").asText());
        String text = document.get("text").asText();
        assertTrue(text.contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(text.contains("Check-in: 2027-03-10"));
        assertTrue(text.contains("BRL"));
        assertTrue(text.contains("2 nights"));
    }

    @Test
    void rejectsReservationWithMultipleIdentificationTypes() throws Exception {
        String invalid = """
                {"roomNumber": 201, "guestName": "Someone", "guestEmail": "test@example.com",
                 "guestCpf": "111.222.333-44", "companyCnpj": "12.345.678/0001-90",
                 "companyName": "Company", "checkIn": "2027-03-10", "checkOut": "2027-03-12",
                 "guests": 2, "ratePlan": "STANDARD"}
                """;
        HttpResponse<String> response = post("/reservations", invalid);
        assertEquals(400, response.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());
    }

    @Test
    void rejectsReservationWithoutIdentification() throws Exception {
        String noId = """
                {"roomNumber": 201, "guestName": "Someone", "guestEmail": "test@example.com",
                 "checkIn": "2027-03-10", "checkOut": "2027-03-12", "guests": 2, "ratePlan": "STANDARD"}
                """;
        HttpResponse<String> response = post("/reservations", noId);
        assertEquals(400, response.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());
    }

    @Test
    void rejectsCompanyReservationWithoutCompanyName() throws Exception {
        String noCnpj = """
                {"roomNumber": 201, "guestName": "Someone", "guestEmail": "test@example.com",
                 "companyCnpj": "12.345.678/0001-90", "checkIn": "2027-03-10",
                 "checkOut": "2027-03-12", "guests": 2, "ratePlan": "STANDARD"}
                """;
        HttpResponse<String> response = post("/reservations", noCnpj);
        assertEquals(400, response.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());
    }
}
