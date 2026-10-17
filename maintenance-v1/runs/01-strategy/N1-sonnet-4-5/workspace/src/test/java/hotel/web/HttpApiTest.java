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

class HttpApiTest {

    private static final String RESERVATION_JSON = """
            {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
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
    void corporateReservationWithValidCompanyCode() throws Exception {
        String corporateJson = """
                {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-10", "checkOut": "2027-03-12",
                 "guests": 2, "ratePlan": "CORPORATE", "companyCode": "TECNOSUL"}
                """;
        HttpResponse<String> response = post("/reservations", corporateJson);

        assertEquals(201, response.statusCode());
        JsonNode reservation = json.readTree(response.body());
        assertEquals(686.22, reservation.get("total").asDouble());
        assertEquals(93.58, reservation.get("discount").asDouble());
    }

    @Test
    void corporateReservationRequiresValidCompanyCode() throws Exception {
        String noCode = """
                {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-10", "checkOut": "2027-03-12",
                 "guests": 2, "ratePlan": "CORPORATE"}
                """;
        HttpResponse<String> response = post("/reservations", noCode);
        assertEquals(400, response.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());

        String invalidCode = """
                {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-10", "checkOut": "2027-03-12",
                 "guests": 2, "ratePlan": "CORPORATE", "companyCode": "INVALID"}
                """;
        response = post("/reservations", invalidCode);
        assertEquals(400, response.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());
    }

    @Test
    void weekendPackageWithCorrectDates() throws Exception {
        String weekendJson = """
                {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-12", "checkOut": "2027-03-14",
                 "guests": 2, "ratePlan": "WEEKEND_PACKAGE"}
                """;
        HttpResponse<String> response = post("/reservations", weekendJson);

        assertEquals(201, response.statusCode());
        JsonNode reservation = json.readTree(response.body());
        assertEquals(820.00, reservation.get("total").asDouble());
        assertEquals(0.00, reservation.get("discount").asDouble());
    }

    @Test
    void weekendPackageRequiresFridayToSunday() throws Exception {
        String wrongDates = """
                {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-10", "checkOut": "2027-03-12",
                 "guests": 2, "ratePlan": "WEEKEND_PACKAGE"}
                """;
        HttpResponse<String> response = post("/reservations", wrongDates);

        assertEquals(400, response.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());
    }

    @Test
    void longStayWith7OrMoreNights() throws Exception {
        String longStayJson = """
                {"roomNumber": 101, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-01", "checkOut": "2027-03-08",
                 "guests": 1, "ratePlan": "LONG_STAY"}
                """;
        HttpResponse<String> response = post("/reservations", longStayJson);

        assertEquals(201, response.statusCode());
        JsonNode reservation = json.readTree(response.body());
        assertEquals(1574.37, reservation.get("total").asDouble());
        assertEquals(174.93, reservation.get("discount").asDouble());
    }

    @Test
    void longStayRequiresAtLeast7Nights() throws Exception {
        String tooShort = """
                {"roomNumber": 101, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-01", "checkOut": "2027-03-07",
                 "guests": 1, "ratePlan": "LONG_STAY"}
                """;
        HttpResponse<String> response = post("/reservations", tooShort);

        assertEquals(400, response.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());
    }
}
