package com.pousadamarealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.config.Database;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationSystemIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static Thread appThread;
    private static final int PORT = 17070;
    private static final String BASE_URL = "http://localhost:" + PORT;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeAll
    static void setup() throws InterruptedException {
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USER", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("PORT", String.valueOf(PORT));

        Database.initialize(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        appThread = new Thread(() -> App.main(new String[]{}));
        appThread.setDaemon(true);
        appThread.start();

        Thread.sleep(2000);
    }

    @Test
    @Order(1)
    void shouldListRooms() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/rooms"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode rooms = objectMapper.readTree(response.body());
        assertEquals(5, rooms.size());

        assertEquals("101", rooms.get(0).get("number").asText());
        assertEquals("STANDARD", rooms.get(0).get("type").asText());
        assertEquals(2, rooms.get(0).get("capacity").asInt());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble(), 0.01);
    }

    @Test
    @Order(2)
    void shouldCreateReservation() throws Exception {
        String requestBody = """
            {
                "roomNumber": "201",
                "guestName": "Carla Dias",
                "guestEmail": "carla@example.com",
                "guestCpf": "11122233344",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        JsonNode reservation = objectMapper.readTree(response.body());

        assertEquals(1, reservation.get("id").asLong());
        assertEquals("201", reservation.get("roomNumber").asText());
        assertEquals("Carla Dias", reservation.get("guestName").asText());
        assertEquals("PENDING", reservation.get("status").asText());
        assertEquals(2, reservation.get("nights").asInt());
        assertEquals(779.80, reservation.get("subtotal").asDouble(), 0.01);
        assertEquals(0.0, reservation.get("discount").asDouble(), 0.01);
        assertEquals(779.80, reservation.get("total").asDouble(), 0.01);
        assertEquals(0.0, reservation.get("amountPaid").asDouble(), 0.01);
    }

    @Test
    @Order(3)
    void shouldConfirmReservationAndSendNotifications() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations/1/confirmation"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        JsonNode reservation = objectMapper.readTree(response.body());
        assertEquals("CONFIRMED", reservation.get("status").asText());
        assertEquals(779.80, reservation.get("amountPaid").asDouble(), 0.01);

        JsonNode messages = getMessages();
        assertEquals(3, messages.size());

        JsonNode guestMessage = messages.get(0);
        assertEquals("EMAIL", guestMessage.get("channel").asText());
        assertEquals("carla@example.com", guestMessage.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestMessage.get("subject").asText());
        assertTrue(guestMessage.get("body").asText().contains("Olá, Carla Dias!"));
        assertTrue(guestMessage.get("body").asText().contains("Comprovante de reserva nº 1"));

        JsonNode housekeepingMessage = messages.get(1);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingMessage.get("to").asText());
        assertEquals("Reserva confirmada - quarto 201", housekeepingMessage.get("subject").asText());
        assertTrue(housekeepingMessage.get("body").asText().contains("Entrada: 10/03/2027"));
        assertTrue(housekeepingMessage.get("body").asText().contains("Hóspedes: 2"));

        JsonNode financeMessage = messages.get(2);
        assertEquals("financeiro@pousadamarealta.com.br", financeMessage.get("to").asText());
        assertEquals("Pagamento recebido - reserva 1", financeMessage.get("subject").asText());
        assertTrue(financeMessage.get("body").asText().contains("R$ 779,80"));
    }

    @Test
    @Order(4)
    void shouldCancelConfirmedReservationAndSendNotifications() throws Exception {
        String requestBody = """
            {
                "roomNumber": "202",
                "guestName": "João Silva",
                "guestEmail": "joao@example.com",
                "guestCpf": "99988877766",
                "checkIn": "2027-04-15",
                "checkOut": "2027-04-16",
                "guests": 1,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        client.send(createRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest confirmRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations/2/confirmation"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        JsonNode messagesBefore = getMessages();
        int messagesBeforeCancel = messagesBefore.size();

        HttpRequest cancelRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations/2/cancellation"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> cancelResponse = client.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cancelResponse.statusCode());

        JsonNode reservation = objectMapper.readTree(cancelResponse.body());
        assertEquals("CANCELLED", reservation.get("status").asText());
        assertEquals(0.0, reservation.get("refund").asDouble(), 0.01);

        JsonNode messagesAfter = getMessages();
        assertEquals(messagesBeforeCancel + 1, messagesAfter.size());

        JsonNode housekeepingMessage = messagesAfter.get(messagesAfter.size() - 1);
        assertEquals("governanca@pousadamarealta.com.br", housekeepingMessage.get("to").asText());
        assertEquals("Reserva cancelada - quarto 202", housekeepingMessage.get("subject").asText());
    }

    @Test
    @Order(5)
    void shouldHandleHighSeasonPricing() throws Exception {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode reservation = objectMapper.readTree(response.body());

        assertEquals(299.88, reservation.get("nightlyRate").asDouble(), 0.01);
        assertEquals(599.76, reservation.get("subtotal").asDouble(), 0.01);
    }

    @Test
    @Order(6)
    void shouldApplyNonRefundableDiscount() throws Exception {
        String requestBody = """
            {
                "roomNumber": "301",
                "guestName": "Pedro Alves",
                "guestEmail": "pedro@example.com",
                "guestCpf": "11111111111",
                "checkIn": "2027-05-01",
                "checkOut": "2027-05-03",
                "guests": 3,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode reservation = objectMapper.readTree(response.body());

        assertEquals(1159.80, reservation.get("subtotal").asDouble(), 0.01);
        assertEquals(173.97, reservation.get("discount").asDouble(), 0.01);
        assertEquals(985.83, reservation.get("total").asDouble(), 0.01);
    }

    @Test
    @Order(7)
    void shouldRejectRoomConflict() throws Exception {
        String requestBody = """
            {
                "roomNumber": "201",
                "guestName": "Conflito",
                "guestEmail": "conflito@example.com",
                "guestCpf": "00000000000",
                "checkIn": "2027-03-11",
                "checkOut": "2027-03-13",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(409, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body());
        assertEquals("ROOM_UNAVAILABLE", error.get("error").asText());
    }

    @Test
    @Order(8)
    void shouldRejectCapacityExceeded() throws Exception {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Muita Gente",
                "guestEmail": "muita@example.com",
                "guestCpf": "22222222222",
                "checkIn": "2027-06-01",
                "checkOut": "2027-06-02",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body());
        assertEquals("CAPACITY_EXCEEDED", error.get("error").asText());
    }

    @Test
    @Order(9)
    void shouldGenerateOccupancyReport() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reports/occupancy?date=2027-03-10"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/csv; charset=utf-8", response.headers().firstValue("content-type").orElse(""));

        String csv = response.body();
        assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        assertTrue(csv.contains("201,DELUXE,1,2027-03-10,2027-03-12"));
        assertTrue(csv.contains("total,1/5"));
    }

    @Test
    @Order(10)
    void shouldGenerateRevenueReport() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/reports/revenue?month=2027-03"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String csv = response.body();
        assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(csv.contains("1,201,2027-03-10,2027-03-12,CONFIRMED,779.80"));
        assertTrue(csv.contains("total,779.80"));
    }

    private JsonNode getMessages() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/messages"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body());
    }
}
