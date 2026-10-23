package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class GuestTypesApiTest {

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
    void acceptsReservationWithCpf() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "Ana Souza", "guestEmail": "ana@example.com",
             "guestCpf": "123.456.789-09", "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> response = post("/reservations", requestJson);
        assertEquals(201, response.statusCode());

        JsonNode reservation = json.readTree(response.body());
        assertEquals("123.456.789-09", reservation.get("guestCpf").asText());
        assertTrue(reservation.get("companyCnpj").isNull());
        assertTrue(reservation.get("guestPassport").isNull());
    }

    @Test
    void acceptsReservationWithCnpj() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "Maria Silva", "guestEmail": "maria@example.com",
             "companyCnpj": "12.345.678/0001-90", "companyName": "Tecnosul Ltda",
             "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> response = post("/reservations", requestJson);
        assertEquals(201, response.statusCode());

        JsonNode reservation = json.readTree(response.body());
        assertEquals("12.345.678/0001-90", reservation.get("companyCnpj").asText());
        assertEquals("Tecnosul Ltda", reservation.get("companyName").asText());
        assertTrue(reservation.get("guestCpf").isNull());
        assertTrue(reservation.get("guestPassport").isNull());
    }

    @Test
    void acceptsReservationWithPassport() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "John Smith", "guestEmail": "john@example.com",
             "guestPassport": "X1234567", "guestCountry": "United Kingdom",
             "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> response = post("/reservations", requestJson);
        assertEquals(201, response.statusCode());

        JsonNode reservation = json.readTree(response.body());
        assertEquals("X1234567", reservation.get("guestPassport").asText());
        assertEquals("United Kingdom", reservation.get("guestCountry").asText());
        assertTrue(reservation.get("guestCpf").isNull());
        assertTrue(reservation.get("companyCnpj").isNull());
    }

    @Test
    void rejectsReservationWithNoIdentification() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "Ana Souza", "guestEmail": "ana@example.com",
             "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> response = post("/reservations", requestJson);
        assertEquals(400, response.statusCode());

        JsonNode error = json.readTree(response.body());
        assertEquals("INVALID_REQUEST", error.get("error").asText());
        assertTrue(error.get("message").asText().contains("exactly one identification type"));
    }

    @Test
    void rejectsReservationWithMultipleIdentifications() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "Ana Souza", "guestEmail": "ana@example.com",
             "guestCpf": "123.456.789-09", "companyCnpj": "12.345.678/0001-90", "companyName": "Tecnosul Ltda",
             "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> response = post("/reservations", requestJson);
        assertEquals(400, response.statusCode());

        JsonNode error = json.readTree(response.body());
        assertEquals("INVALID_REQUEST", error.get("error").asText());
    }

    @Test
    void rejectsReservationWithIncompleteCompanyInfo() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "Maria Silva", "guestEmail": "maria@example.com",
             "companyCnpj": "12.345.678/0001-90",
             "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> response = post("/reservations", requestJson);
        assertEquals(400, response.statusCode());

        JsonNode error = json.readTree(response.body());
        assertEquals("INVALID_REQUEST", error.get("error").asText());
    }

    @Test
    void rejectsReservationWithIncompletePassportInfo() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "John Smith", "guestEmail": "john@example.com",
             "guestPassport": "X1234567",
             "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> response = post("/reservations", requestJson);
        assertEquals(400, response.statusCode());

        JsonNode error = json.readTree(response.body());
        assertEquals("INVALID_REQUEST", error.get("error").asText());
    }

    @Test
    void getReservationIncludesDocumentForPaidReservations() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "Ana Souza", "guestEmail": "ana@example.com",
             "guestCpf": "123.456.789-09", "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> created = post("/reservations", requestJson);
        JsonNode reservation = json.readTree(created.body());
        long id = reservation.get("id").asLong();

        HttpResponse<String> beforeConfirm = get("/reservations/" + id);
        JsonNode beforeDoc = json.readTree(beforeConfirm.body());
        assertTrue(beforeDoc.get("document").isNull());

        post("/reservations/" + id + "/confirmation", "");

        HttpResponse<String> afterConfirm = get("/reservations/" + id);
        JsonNode afterDoc = json.readTree(afterConfirm.body());
        JsonNode document = afterDoc.get("document");

        assertEquals("Comprovante de reserva", document.get("title").asText());
        assertEquals("123.456.789-09", document.get("guestCpf").asText());
        assertTrue(document.get("text").asText().contains("Comprovante de reserva nº " + id));
        assertTrue(document.get("text").asText().contains("Total: R$ 249,90"));
    }

    @Test
    void confirmationReturnsDocumentForCompany() throws Exception {
        String requestJson = """
            {"roomNumber": 201, "guestName": "Maria Silva", "guestEmail": "maria@example.com",
             "companyCnpj": "12.345.678/0001-90", "companyName": "Tecnosul Ltda",
             "checkIn": "2027-03-10", "checkOut": "2027-03-12",
             "guests": 2, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> created = post("/reservations", requestJson);
        JsonNode reservation = json.readTree(created.body());
        long id = reservation.get("id").asLong();

        HttpResponse<String> confirmed = post("/reservations/" + id + "/confirmation", "");
        JsonNode response = json.readTree(confirmed.body());
        JsonNode document = response.get("document");

        assertEquals("Fatura de hospedagem", document.get("title").asText());
        assertEquals("12.345.678/0001-90", document.get("companyCnpj").asText());
        assertEquals("Tecnosul Ltda", document.get("companyName").asText());
        assertTrue(document.get("text").asText().contains("ISS retido (5%)"));
    }

    @Test
    void checkOutReturnsDocumentForForeignGuest() throws Exception {
        String requestJson = """
            {"roomNumber": 101, "guestName": "John Smith", "guestEmail": "john@example.com",
             "guestPassport": "X1234567", "guestCountry": "United Kingdom",
             "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> created = post("/reservations", requestJson);
        JsonNode reservation = json.readTree(created.body());
        long id = reservation.get("id").asLong();

        post("/reservations/" + id + "/confirmation", "");
        post("/reservations/" + id + "/check-in", "");

        HttpResponse<String> checkedOut = post("/reservations/" + id + "/check-out", "");
        JsonNode document = json.readTree(checkedOut.body());

        assertEquals("Booking receipt", document.get("title").asText());
        assertEquals("X1234567", document.get("guestPassport").asText());
        assertEquals("United Kingdom", document.get("guestCountry").asText());
        assertTrue(document.get("text").asText().contains("Guest: John Smith (passport X1234567, United Kingdom)"));
        assertTrue(document.get("text").asText().contains("BRL 249.90"));
    }

    @Test
    void getDocumentReturnsCorrectReceiptForEachType() throws Exception {
        String cpfRequest = """
            {"roomNumber": 101, "guestName": "Ana", "guestEmail": "ana@example.com",
             "guestCpf": "123.456.789-09", "checkIn": "2027-03-10", "checkOut": "2027-03-11",
             "guests": 1, "ratePlan": "STANDARD"}
            """;

        HttpResponse<String> created = post("/reservations", cpfRequest);
        long id = json.readTree(created.body()).get("id").asLong();
        post("/reservations/" + id + "/confirmation", "");

        HttpResponse<String> documentResponse = get("/reservations/" + id + "/document");
        JsonNode document = json.readTree(documentResponse.body());

        assertEquals("Comprovante de reserva", document.get("title").asText());
        assertTrue(document.get("text").asText().contains("Comprovante de reserva"));
    }
}
