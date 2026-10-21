package hotel.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hotel.Hotel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentTypesTest {

    private static final String COMMON = """
            "roomNumber": 201, "guestName": "%s", "guestEmail": "g@example.com",
            "checkIn": "2027-03-10", "checkOut": "2027-03-12", "guests": 2, "ratePlan": "STANDARD"
            """;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private Hotel hotel;
    private String baseUrl;

    @BeforeEach
    void start() {
        hotel = new Hotel();
        baseUrl = "http://localhost:" + hotel.start(0);
    }

    @AfterEach
    void stop() {
        hotel.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json");
        request.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode createAndConfirm(String guestName, String identification) throws Exception {
        String body = "{" + COMMON.formatted(guestName) + "," + identification + "}";
        HttpResponse<String> created = send("POST", "/reservations", body);
        assertEquals(201, created.statusCode(), created.body());
        long id = json.readTree(created.body()).get("id").asLong();
        assertEquals(200, send("POST", "/reservations/" + id + "/confirmation", null).statusCode());
        return json.readTree(send("GET", "/reservations/" + id + "/document", null).body());
    }

    @Test
    void companyGetsAnInvoiceWithWithheldIss() throws Exception {
        JsonNode document = createAndConfirm(
                "Ana Souza", "\"companyCnpj\": \"12.345.678/0001-90\", \"companyName\": \"Tecnosul Ltda\"");

        assertEquals("Fatura de hospedagem", document.get("title").asText());
        assertTrue(document.get("guestCpf").isNull());
        assertEquals("12.345.678/0001-90", document.get("companyCnpj").asText());
        assertEquals("Tecnosul Ltda", document.get("companyName").asText());
        String text = document.get("text").asText();
        assertTrue(text.startsWith("Fatura de hospedagem nº 1\nEmpresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)\n"
                + "Hóspede: Ana Souza\nQuarto: 201 (DELUXE)\n"), text);
        assertTrue(text.contains("Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\n"), text);
        assertTrue(text.endsWith("Total: R$ 779,80\nISS retido (5%): R$ 38,99\n"), text);
    }

    @Test
    void foreignGuestGetsAnEnglishBookingReceipt() throws Exception {
        JsonNode document = createAndConfirm(
                "John Smith", "\"guestPassport\": \"X1234567\", \"guestCountry\": \"United Kingdom\"");

        assertEquals("""
                Booking receipt no. 1
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
                Nights: BRL 779.80
                Total: BRL 779.80
                """, document.get("text").asText());
        assertEquals("X1234567", document.get("guestPassport").asText());
        assertTrue(document.get("companyCnpj").isNull());
    }

    @Test
    void cpfGuestKeepsTheReceipt() throws Exception {
        JsonNode document = createAndConfirm("Carla Dias", "\"guestCpf\": \"111.222.333-44\"");

        assertEquals("Comprovante de reserva", document.get("title").asText());
        assertTrue(document.get("text").asText().contains("Hóspede: Carla Dias (CPF 111.222.333-44)\n"));
    }

    @Test
    void reservationShowsIdentificationFieldsWithNullsForTheOthers() throws Exception {
        String body = "{" + COMMON.formatted("John") + ", \"guestPassport\": \"P1\", \"guestCountry\": \"UK\"}";
        long id = json.readTree(send("POST", "/reservations", body).body()).get("id").asLong();
        JsonNode reservation = json.readTree(send("GET", "/reservations/" + id, null).body());

        assertEquals("P1", reservation.get("guestPassport").asText());
        assertEquals("UK", reservation.get("guestCountry").asText());
        assertTrue(reservation.get("guestCpf").isNull());
        assertTrue(reservation.get("companyCnpj").isNull());
        assertTrue(reservation.get("companyName").isNull());
    }

    @Test
    void identificationMustBeExactlyOneKind() throws Exception {
        for (String identification : new String[] {
                "\"x\": 1",
                "\"companyCnpj\": \"1\"",
                "\"companyName\": \"Acme\"",
                "\"guestPassport\": \"P1\"",
                "\"guestCountry\": \"UK\"",
                "\"guestCpf\": \"1\", \"guestPassport\": \"P1\", \"guestCountry\": \"UK\"",
                "\"guestCpf\": \"1\", \"companyCnpj\": \"2\", \"companyName\": \"Acme\"",
        }) {
            HttpResponse<String> response =
                    send("POST", "/reservations", "{" + COMMON.formatted("X") + "," + identification + "}");
            assertEquals(400, response.statusCode(), identification);
            assertEquals("INVALID_REQUEST", json.readTree(response.body()).get("error").asText());
        }
    }
}
