package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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
        assertEquals(3, messages.size());
        assertEquals("carla@example.com", messages.get(0).get("to").asText());
        assertEquals("governanca@pousadamarealta.com.br", messages.get(1).get("to").asText());
        assertEquals("financeiro@pousadamarealta.com.br", messages.get(2).get("to").asText());

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
    void confirmationNotifiesGovernanceWithCorrectContent() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        JsonNode governanceMessage = messages.get(1);
        assertEquals("governanca@pousadamarealta.com.br", governanceMessage.get("to").asText());
        assertEquals("Reserva confirmada - quarto 201", governanceMessage.get("subject").asText());
        assertTrue(governanceMessage.get("body").asText().contains("Entrada: 10/07/2027"));
        assertTrue(governanceMessage.get("body").asText().contains("Saída: 12/07/2027"));
        assertTrue(governanceMessage.get("body").asText().contains("Hóspedes: 2"));
    }

    @Test
    void confirmationNotifiesFinanceWithCorrectContent() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        JsonNode financeMessage = messages.get(2);
        assertEquals("financeiro@pousadamarealta.com.br", financeMessage.get("to").asText());
        assertEquals("Pagamento recebido - reserva " + id, financeMessage.get("subject").asText());
        assertTrue(financeMessage.get("body").asText().contains("935,76"));
    }

    @Test
    void cancellationOfConfirmedReservationNotifiesGovernance() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");
        get("/messages");
        post("/reservations/" + id + "/cancellation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        boolean foundGovernanceCancel = false;
        for (int i = 0; i < messages.size(); i++) {
            JsonNode msg = messages.get(i);
            if ("governanca@pousadamarealta.com.br".equals(msg.get("to").asText())
                    && msg.get("subject").asText().contains("cancelada")) {
                foundGovernanceCancel = true;
                assertTrue(msg.get("body").asText().contains("10/07/2027"));
                assertTrue(msg.get("body").asText().contains("12/07/2027"));
            }
        }
        assertTrue(foundGovernanceCancel);
    }

    @Test
    void cancellationOfConfirmedReservationWithRefundNotifiesFinance() throws Exception {
        String standardReservation = """
                {"roomNumber": 101, "guestName": "Anna Silva", "guestEmail": "anna@example.com",
                 "guestCpf": "222.333.444-55", "checkIn": "2027-05-10", "checkOut": "2027-05-12",
                 "guests": 1, "ratePlan": "STANDARD"}
                """;
        HttpResponse<String> created = post("/reservations", standardReservation);
        assertEquals(201, created.statusCode());
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");
        post("/reservations/" + id + "/cancellation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        boolean foundFinanceRefund = false;
        for (int i = 0; i < messages.size(); i++) {
            JsonNode msg = messages.get(i);
            if ("financeiro@pousadamarealta.com.br".equals(msg.get("to").asText())
                    && msg.get("subject").asText().contains("Reembolso")) {
                foundFinanceRefund = true;
                assertTrue(msg.get("body").asText().contains("R$"));
            }
        }
        assertTrue(foundFinanceRefund);
    }

    @Test
    void cancellationOfNonRefundableReservationDoesNotNotifyFinance() throws Exception {
        String nonRefundableReservation = """
                {"roomNumber": 102, "guestName": "Bob Santos", "guestEmail": "bob@example.com",
                 "guestCpf": "333.444.555-66", "checkIn": "2027-06-10", "checkOut": "2027-06-12",
                 "guests": 1, "ratePlan": "NON_REFUNDABLE"}
                """;
        HttpResponse<String> created = post("/reservations", nonRefundableReservation);
        assertEquals(201, created.statusCode());
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");
        post("/reservations/" + id + "/cancellation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        for (int i = 0; i < messages.size(); i++) {
            JsonNode msg = messages.get(i);
            if ("financeiro@pousadamarealta.com.br".equals(msg.get("to").asText())
                    && msg.get("subject").asText().contains("Reembolso")) {
                fail("Finance should not be notified for non-refundable cancellation");
            }
        }
    }

    @Test
    void cancellationOfPendingReservationDoesNotNotifyAnyone() throws Exception {
        HttpResponse<String> created = post("/reservations", RESERVATION_JSON);
        long id = json.readTree(created.body()).get("id").asLong();

        post("/reservations/" + id + "/cancellation", "");

        JsonNode messages = json.readTree(get("/messages").body());
        assertEquals(0, messages.size());
    }
}
