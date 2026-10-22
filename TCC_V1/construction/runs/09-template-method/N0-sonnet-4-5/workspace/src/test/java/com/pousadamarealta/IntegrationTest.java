package com.pousadamarealta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.db.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.web.ApiController;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {
    private Javalin app;
    private String baseUrl;
    private ObjectMapper objectMapper;
    private HttpClient httpClient;

    @BeforeAll
    void setup() {
        String dbUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        String dbUser = "sa";
        String dbPassword = "";

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        ReservationRepository reservationRepository = new ReservationRepository(database);
        MessageRepository messageRepository = new MessageRepository(database);

        PricingService pricingService = new PricingService();
        NotificationService notificationService = new NotificationService(messageRepository);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomRepository, pricingService, notificationService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
                roomRepository, reservationService, messageRepository, reportService);

        app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        });

        apiController.registerRoutes(app);
        app.start(0);

        baseUrl = "http://localhost:" + app.port();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    void teardown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void testGetRooms() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rooms"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        List<Map<String, Object>> rooms = objectMapper.readValue(response.body(),
                new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(5, rooms.size());

        Map<String, Object> room101 = rooms.get(0);
        assertEquals("101", room101.get("number"));
        assertEquals("STANDARD", room101.get("type"));
        assertEquals(2, room101.get("capacity"));
        assertEquals(249.90, ((Number) room101.get("nightlyRate")).doubleValue(), 0.01);
    }

    @Test
    void testCreateAndConfirmReservation() throws Exception {
        Map<String, Object> requestBody = Map.of(
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
                .uri(URI.create(baseUrl + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();
        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, createResponse.statusCode());
        Map<String, Object> reservation = objectMapper.readValue(createResponse.body(),
                new TypeReference<Map<String, Object>>() {});

        assertEquals(1L, ((Number) reservation.get("id")).longValue());
        assertEquals("PENDING", reservation.get("status"));
        assertEquals(2, reservation.get("nights"));
        assertEquals(0.0, ((Number) reservation.get("amountPaid")).doubleValue(), 0.01);

        Long id = ((Number) reservation.get("id")).longValue();
        HttpRequest confirmRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reservations/" + id + "/confirmation"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> confirmResponse = httpClient.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, confirmResponse.statusCode());
        Map<String, Object> confirmed = objectMapper.readValue(confirmResponse.body(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals("CONFIRMED", confirmed.get("status"));
        assertEquals(((Number) confirmed.get("total")).doubleValue(),
                ((Number) confirmed.get("amountPaid")).doubleValue(), 0.01);
    }

    @Test
    void testHighSeasonPricing() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roomNumber", "101",
                "guestName", "Maria Santos",
                "guestEmail", "maria@example.com",
                "guestCpf", "98765432109",
                "checkIn", "2027-07-10",
                "checkOut", "2027-07-12",
                "guests", 1,
                "ratePlan", "STANDARD"
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Map<String, Object> reservation = objectMapper.readValue(response.body(),
                new TypeReference<Map<String, Object>>() {});

        assertEquals(599.76, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(599.76, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    void testNonRefundableDiscount() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roomNumber", "201",
                "guestName", "Ana Costa",
                "guestEmail", "ana@example.com",
                "guestCpf", "11122233344",
                "checkIn", "2027-03-10",
                "checkOut", "2027-03-12",
                "guests", 2,
                "ratePlan", "NON_REFUNDABLE"
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Map<String, Object> reservation = objectMapper.readValue(response.body(),
                new TypeReference<Map<String, Object>>() {});

        assertEquals(779.80, ((Number) reservation.get("subtotal")).doubleValue(), 0.01);
        assertEquals(116.97, ((Number) reservation.get("discount")).doubleValue(), 0.01);
        assertEquals(662.83, ((Number) reservation.get("total")).doubleValue(), 0.01);
    }

    @Test
    void testRoomConflict() throws Exception {
        Map<String, Object> request1Body = Map.of(
                "roomNumber", "102",
                "guestName", "Pedro Lima",
                "guestEmail", "pedro@example.com",
                "guestCpf", "55566677788",
                "checkIn", "2027-04-10",
                "checkOut", "2027-04-15",
                "guests", 2,
                "ratePlan", "STANDARD"
        );

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request1Body)))
                .build();
        HttpResponse<String> response1 = httpClient.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());

        Map<String, Object> request2Body = Map.of(
                "roomNumber", "102",
                "guestName", "Carlos Souza",
                "guestEmail", "carlos@example.com",
                "guestCpf", "99988877766",
                "checkIn", "2027-04-12",
                "checkOut", "2027-04-17",
                "guests", 1,
                "ratePlan", "STANDARD"
        );

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request2Body)))
                .build();
        HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());

        assertEquals(409, response2.statusCode());
        Map<String, Object> error = objectMapper.readValue(response2.body(),
                new TypeReference<Map<String, Object>>() {});
        assertEquals("ROOM_UNAVAILABLE", error.get("error"));
    }

    @Test
    void testReports() throws Exception {
        HttpRequest occupancyRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reports/occupancy?date=2027-07-10"))
                .GET()
                .build();
        HttpResponse<String> occupancyResponse = httpClient.send(occupancyRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, occupancyResponse.statusCode());
        assertTrue(occupancyResponse.body().contains("Pousada Maré Alta,occupancy,2027-07-10"));

        HttpRequest revenueRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reports/revenue?month=2027-03"))
                .GET()
                .build();
        HttpResponse<String> revenueResponse = httpClient.send(revenueRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, revenueResponse.statusCode());
        assertTrue(revenueResponse.body().contains("Pousada Maré Alta,revenue,2027-03"));

        HttpRequest taxRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reports/tourism-tax?month=2027-03"))
                .GET()
                .build();
        HttpResponse<String> taxResponse = httpClient.send(taxRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, taxResponse.statusCode());
        assertTrue(taxResponse.body().contains("Pousada Maré Alta,tourism-tax,2027-03"));

        HttpRequest plansRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/reports/rate-plans?month=2027-03"))
                .GET()
                .build();
        HttpResponse<String> plansResponse = httpClient.send(plansRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, plansResponse.statusCode());
        assertTrue(plansResponse.body().contains("Pousada Maré Alta,rate-plans,2027-03"));
        assertTrue(plansResponse.body().contains("STANDARD,"));
        assertTrue(plansResponse.body().contains("NON_REFUNDABLE,"));
    }
}
