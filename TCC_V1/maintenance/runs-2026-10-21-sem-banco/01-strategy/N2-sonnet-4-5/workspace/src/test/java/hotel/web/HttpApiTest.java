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
    void supportsNewRatePlans() throws Exception {
        // Corporate plan with company code
        String corporateReservation = """
                {"roomNumber": 201, "guestName": "Carla Dias", "guestEmail": "carla@example.com",
                 "guestCpf": "111.222.333-44", "checkIn": "2027-03-10", "checkOut": "2027-03-12",
                 "guests": 2, "ratePlan": "CORPORATE", "companyCode": "TECNOSUL"}
                """;
        HttpResponse<String> corporate = post("/reservations", corporateReservation);
        assertEquals(201, corporate.statusCode());
        JsonNode corporateData = json.readTree(corporate.body());
        assertEquals(779.80, corporateData.get("subtotal").asDouble());
        assertEquals(93.58, corporateData.get("discount").asDouble());
        assertEquals(686.22, corporateData.get("total").asDouble());

        // Weekend package
        String weekendReservation = """
                {"roomNumber": 202, "guestName": "Pedro Silva", "guestEmail": "pedro@example.com",
                 "guestCpf": "222.333.444-55", "checkIn": "2027-03-12", "checkOut": "2027-03-14",
                 "guests": 2, "ratePlan": "WEEKEND_PACKAGE"}
                """;
        HttpResponse<String> weekend = post("/reservations", weekendReservation);
        assertEquals(201, weekend.statusCode());
        assertEquals(820.00, json.readTree(weekend.body()).get("total").asDouble());

        // Long stay
        String longStayReservation = """
                {"roomNumber": 101, "guestName": "Maria Costa", "guestEmail": "maria@example.com",
                 "guestCpf": "333.444.555-66", "checkIn": "2027-03-01", "checkOut": "2027-03-08",
                 "guests": 2, "ratePlan": "LONG_STAY"}
                """;
        HttpResponse<String> longStay = post("/reservations", longStayReservation);
        assertEquals(201, longStay.statusCode());
        JsonNode longStayData = json.readTree(longStay.body());
        assertEquals(1749.30, longStayData.get("subtotal").asDouble());
        assertEquals(174.93, longStayData.get("discount").asDouble());
        assertEquals(1574.37, longStayData.get("total").asDouble());

        // Corporate plan without company code should fail
        String noCompanyCode = """
                {"roomNumber": 102, "guestName": "João Santos", "guestEmail": "joao@example.com",
                 "guestCpf": "444.555.666-77", "checkIn": "2027-03-10", "checkOut": "2027-03-12",
                 "guests": 2, "ratePlan": "CORPORATE"}
                """;
        HttpResponse<String> invalid = post("/reservations", noCompanyCode);
        assertEquals(400, invalid.statusCode());
        assertEquals("INVALID_REQUEST", json.readTree(invalid.body()).get("error").asText());
    }
}
