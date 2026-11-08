package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.event.EventPublisher;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.notification.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SystemIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    private Javalin app;
    private HttpClient client;
    private ObjectMapper objectMapper;
    private int port = 17070;

    @BeforeAll
    void setUp() {
        Database.init(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        PricingService pricingService = new PricingService();
        NotificationService notificationService = new NotificationService(messageRepository);
        ReportService reportService = new ReportService(reservationRepository);

        EventPublisher eventPublisher = new EventPublisher();

        eventPublisher.subscribe(ReservationConfirmedEvent.class,
            new GuestNotificationListener(notificationService));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
            new HousekeepingNotificationListener(notificationService));
        eventPublisher.subscribe(ReservationConfirmedEvent.class,
            new FinanceNotificationListener(notificationService));

        eventPublisher.subscribe(ReservationCancelledEvent.class,
            new HousekeepingCancellationListener(notificationService));
        eventPublisher.subscribe(ReservationCancelledEvent.class,
            new FinanceCancellationListener(notificationService));

        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, eventPublisher);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).start(port);

        app.get("/rooms", roomController::getRooms);

        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkIn);
        app.post("/reservations/{id}/check-out", reservationController::checkOut);
        app.get("/reservations/{id}/document", reservationController::getDocument);

        app.get("/messages", messageController::getMessages);

        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);

        client = HttpClient.newHttpClient();
    }

    @AfterAll
    void tearDown() {
        app.stop();
        Database.close();
    }

    @Test
    void shouldGetRooms() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/rooms"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        List<Map> rooms = objectMapper.readValue(response.body(), List.class);
        assertEquals(5, rooms.size());
        assertEquals("101", rooms.get(0).get("number"));
        assertEquals("STANDARD", rooms.get(0).get("type"));
        assertEquals(2, rooms.get(0).get("capacity"));
        assertEquals(249.90, rooms.get(0).get("nightlyRate"));
    }

    @Test
    void shouldCreateAndConfirmReservationWithNotifications() throws Exception {
        Map<String, Object> reservationData = Map.of(
            "roomNumber", "101",
            "guestName", "João Silva",
            "guestEmail", "joao@example.com",
            "guestCpf", "12345678901",
            "checkIn", "2027-03-10",
            "checkOut", "2027-03-12",
            "guests", 2,
            "ratePlan", "STANDARD"
        );

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reservationData)))
            .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResponse.statusCode());

        Map reservation = objectMapper.readValue(createResponse.body(), Map.class);
        assertEquals("PENDING", reservation.get("status"));
        assertEquals(499.80, ((Number) reservation.get("total")).doubleValue(), 0.01);

        Integer reservationId = (Integer) reservation.get("id");

        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations/" + reservationId + "/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> confirmResponse = client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, confirmResponse.statusCode());

        Map confirmed = objectMapper.readValue(confirmResponse.body(), Map.class);
        assertEquals("CONFIRMED", confirmed.get("status"));
        assertEquals(499.80, ((Number) confirmed.get("amountPaid")).doubleValue(), 0.01);

        HttpRequest messagesRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesResponse = client.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        List<Map> messages = objectMapper.readValue(messagesResponse.body(), List.class);

        assertEquals(3, messages.size(), "Should have 3 notifications: guest, housekeeping, and finance");

        Map guestMessage = messages.stream()
            .filter(m -> "joao@example.com".equals(m.get("to")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Guest notification not found"));
        assertEquals("EMAIL", guestMessage.get("channel"));
        assertEquals("Reserva confirmada - Pousada Maré Alta", guestMessage.get("subject"));
        assertTrue(((String) guestMessage.get("body")).contains("João Silva"));
        assertTrue(((String) guestMessage.get("body")).contains("Comprovante de reserva"));

        Map housekeepingMessage = messages.stream()
            .filter(m -> "governanca@pousadamarealta.com.br".equals(m.get("to")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Housekeeping notification not found"));
        assertEquals("Reserva confirmada - quarto 101", housekeepingMessage.get("subject"));
        assertTrue(((String) housekeepingMessage.get("body")).contains("Entrada:"));
        assertTrue(((String) housekeepingMessage.get("body")).contains("Hóspedes: 2"));

        Map financeMessage = messages.stream()
            .filter(m -> "financeiro@pousadamarealta.com.br".equals(m.get("to")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Finance notification not found"));
        assertTrue(((String) financeMessage.get("subject")).contains("Pagamento recebido"));
        assertTrue(((String) financeMessage.get("body")).contains("R$ 499,80"));
    }

    @Test
    void shouldHandleHighSeasonPricing() throws Exception {
        Map<String, Object> reservationData = Map.of(
            "roomNumber", "102",
            "guestName", "Maria Santos",
            "guestEmail", "maria@example.com",
            "guestCpf", "98765432100",
            "checkIn", "2027-07-10",
            "checkOut", "2027-07-12",
            "guests", 2,
            "ratePlan", "STANDARD"
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reservationData)))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Map reservation = objectMapper.readValue(response.body(), Map.class);

        assertEquals(599.76, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    void shouldApplyNonRefundableDiscount() throws Exception {
        Map<String, Object> reservationData = Map.of(
            "roomNumber", "201",
            "guestName", "Pedro Costa",
            "guestEmail", "pedro@example.com",
            "guestCpf", "11122233344",
            "checkIn", "2027-03-10",
            "checkOut", "2027-03-12",
            "guests", 2,
            "ratePlan", "NON_REFUNDABLE"
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reservationData)))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Map reservation = objectMapper.readValue(response.body(), Map.class);

        assertEquals(779.80, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(116.97, ((Number) reservation.get("discount")).doubleValue(), 0.01);
        assertEquals(662.83, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    void shouldNotifyOnCancellationWithRefund() throws Exception {
        Map<String, Object> reservationData = Map.of(
            "roomNumber", "202",
            "guestName", "Ana Lima",
            "guestEmail", "ana@example.com",
            "guestCpf", "55566677788",
            "checkIn", "2027-04-10",
            "checkOut", "2027-04-12",
            "guests", 2,
            "ratePlan", "STANDARD"
        );

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reservationData)))
            .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        Map reservation = objectMapper.readValue(createResponse.body(), Map.class);
        Integer reservationId = (Integer) reservation.get("id");

        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations/" + reservationId + "/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        int messageCountBefore = getMessageCount();

        HttpRequest cancelRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations/" + reservationId + "/cancellation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> cancelResponse = client.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
        Map cancelled = objectMapper.readValue(cancelResponse.body(), Map.class);

        assertEquals("CANCELLED", cancelled.get("status"));
        assertEquals(779.80, ((Number) cancelled.get("refund")).doubleValue(), 0.01);

        HttpRequest messagesRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesResponse = client.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        List<Map> messages = objectMapper.readValue(messagesResponse.body(), List.class);

        boolean hasHousekeepingCancellation = messages.stream()
            .skip(messageCountBefore)
            .anyMatch(m -> "governanca@pousadamarealta.com.br".equals(m.get("to")) &&
                          ((String) m.get("subject")).contains("Reserva cancelada"));
        assertTrue(hasHousekeepingCancellation, "Should have housekeeping cancellation notification");

        boolean hasFinanceRefund = messages.stream()
            .skip(messageCountBefore)
            .anyMatch(m -> "financeiro@pousadamarealta.com.br".equals(m.get("to")) &&
                          ((String) m.get("subject")).contains("Reembolso"));
        assertTrue(hasFinanceRefund, "Should have finance refund notification");
    }

    @Test
    void shouldNotNotifyFinanceOnNonRefundableCancellation() throws Exception {
        Map<String, Object> reservationData = Map.of(
            "roomNumber", "301",
            "guestName", "Carlos Souza",
            "guestEmail", "carlos@example.com",
            "guestCpf", "99988877766",
            "checkIn", "2027-05-10",
            "checkOut", "2027-05-12",
            "guests", 2,
            "ratePlan", "NON_REFUNDABLE"
        );

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reservationData)))
            .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        Map reservation = objectMapper.readValue(createResponse.body(), Map.class);
        Integer reservationId = (Integer) reservation.get("id");

        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations/" + reservationId + "/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        int messageCountBefore = getMessageCount();

        HttpRequest cancelRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/reservations/" + reservationId + "/cancellation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> cancelResponse = client.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
        Map cancelled = objectMapper.readValue(cancelResponse.body(), Map.class);
        assertEquals(0.0, ((Number) cancelled.get("refund")).doubleValue(), 0.01);

        HttpRequest messagesRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesResponse = client.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        List<Map> messages = objectMapper.readValue(messagesResponse.body(), List.class);

        int newMessages = messages.size() - messageCountBefore;
        assertEquals(1, newMessages, "Should only have housekeeping cancellation, no finance refund");

        boolean hasFinanceRefund = messages.stream()
            .skip(messageCountBefore)
            .anyMatch(m -> "financeiro@pousadamarealta.com.br".equals(m.get("to")) &&
                          ((String) m.get("subject")).contains("Reembolso"));
        assertFalse(hasFinanceRefund, "Should not notify finance when there's no refund");
    }

    private int getMessageCount() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/messages"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Map> messages = objectMapper.readValue(response.body(), List.class);
        return messages.size();
    }
}
