package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.MessageController;
import com.pousadamarealta.api.ReportController;
import com.pousadamarealta.api.ReservationController;
import com.pousadamarealta.api.RoomController;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.domain.event.EventBus;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.event.handlers.FinanceNotificationHandler;
import com.pousadamarealta.domain.event.handlers.GuestNotificationHandler;
import com.pousadamarealta.domain.event.handlers.HousekeepingNotificationHandler;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.service.ReportService;
import com.pousadamarealta.domain.service.ReservationService;
import com.pousadamarealta.domain.service.RoomService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationSystemIT {

    private static Javalin app;
    private static HttpClient httpClient;
    private static String baseUrl;
    private static Database database;

    @BeforeAll
    static void setup() throws Exception {
        String dbUrl = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        database = new Database(dbUrl, "sa", "");
        database.initializeSchema();

        try (Connection conn = database.getConnection()) {
            RoomRepository roomRepository = new RoomRepository(conn);
            roomRepository.initializeRooms();
        }

        EventBus eventBus = new EventBus();

        Connection connection = database.getConnection();
        MessageRepository messageRepository = new MessageRepository(connection);
        ReservationRepository reservationRepository = new ReservationRepository(connection);
        RoomRepository roomRepository = new RoomRepository(connection);

        MessageService messageService = new MessageService(messageRepository);
        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(reservationRepository, roomService, eventBus);
        ReportService reportService = new ReportService(reservationRepository);

        GuestNotificationHandler guestHandler = new GuestNotificationHandler(messageService);
        HousekeepingNotificationHandler housekeepingHandler = new HousekeepingNotificationHandler(messageService);
        FinanceNotificationHandler financeHandler = new FinanceNotificationHandler(messageService);

        eventBus.subscribe(ReservationConfirmedEvent.class, guestHandler);
        eventBus.subscribe(ReservationConfirmedEvent.class, housekeepingHandler.onConfirmed());
        eventBus.subscribe(ReservationConfirmedEvent.class, financeHandler.onConfirmed());
        eventBus.subscribe(ReservationCancelledEvent.class, housekeepingHandler.onCancelled());
        eventBus.subscribe(ReservationCancelledEvent.class, financeHandler.onCancelled());

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(0);

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

        baseUrl = "http://localhost:" + app.port();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
        }
        if (database != null) {
            database.close();
        }
    }

    @Test
    @Order(1)
    void testGetRooms() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/rooms"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("101"));
        assertTrue(response.body().contains("STANDARD"));
        assertTrue(response.body().contains("249.90"));
    }

    @Test
    @Order(2)
    void testCreateReservation() throws Exception {
        String json = """
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
            .uri(URI.create(baseUrl + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"id\":1"));
        assertTrue(response.body().contains("\"status\":\"PENDING\""));
        assertTrue(response.body().contains("\"total\":779.80"));
    }

    @Test
    @Order(3)
    void testConfirmReservationSendsNotifications() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations/1/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"CONFIRMED\""));

        HttpRequest messagesRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesResponse = httpClient.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        String messages = messagesResponse.body();

        assertTrue(messages.contains("carla@example.com"));
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"));
        assertTrue(messages.contains("governanca@pousadamarealta.com.br"));
        assertTrue(messages.contains("Reserva confirmada - quarto 201"));
        assertTrue(messages.contains("financeiro@pousadamarealta.com.br"));
        assertTrue(messages.contains("Pagamento recebido - reserva 1"));
    }

    @Test
    @Order(4)
    void testHighSeasonPricing() throws Exception {
        String json = """
            {
                "roomNumber": "101",
                "guestName": "João Silva",
                "guestEmail": "joao@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"subtotal\":599.76"));
        assertTrue(response.body().contains("\"total\":599.76"));
    }

    @Test
    @Order(5)
    void testNonRefundablePlan() throws Exception {
        String json = """
            {
                "roomNumber": "102",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "98765432109",
                "checkIn": "2027-03-15",
                "checkOut": "2027-03-16",
                "guests": 1,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"discount\":37.48"));
        assertTrue(response.body().contains("\"total\":212.42"));
    }

    @Test
    @Order(6)
    void testRoomUnavailable() throws Exception {
        String json = """
            {
                "roomNumber": "201",
                "guestName": "Pedro Oliveira",
                "guestEmail": "pedro@example.com",
                "guestCpf": "11111111111",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("ROOM_UNAVAILABLE"));
    }

    @Test
    @Order(7)
    void testCapacityExceeded() throws Exception {
        String json = """
            {
                "roomNumber": "101",
                "guestName": "Ana Costa",
                "guestEmail": "ana@example.com",
                "guestCpf": "22222222222",
                "checkIn": "2027-04-01",
                "checkOut": "2027-04-02",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("CAPACITY_EXCEEDED"));
    }

    @Test
    @Order(8)
    void testCancellationWithRefund() throws Exception {
        HttpRequest cancelRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations/1/cancellation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> cancelResponse = httpClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cancelResponse.statusCode());
        assertTrue(cancelResponse.body().contains("\"status\":\"CANCELLED\""));
        assertTrue(cancelResponse.body().contains("\"refund\":779.80"));

        HttpRequest messagesRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesResponse = httpClient.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        String messages = messagesResponse.body();

        assertTrue(messages.contains("Reserva cancelada - quarto 201"));
        assertTrue(messages.contains("Reembolso - reserva 1"));
    }

    @Test
    @Order(9)
    void testCancellationNonRefundable() throws Exception {
        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations/3/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        httpClient.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        int messageCountBefore = getMessageCount();

        HttpRequest cancelRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations/3/cancellation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> cancelResponse = httpClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cancelResponse.statusCode());
        assertTrue(cancelResponse.body().contains("\"refund\":0"));

        int messageCountAfter = getMessageCount();
        assertEquals(messageCountBefore + 1, messageCountAfter);
    }

    @Test
    @Order(10)
    void testOccupancyReport() throws Exception {
        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reservations/2/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        httpClient.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reports/occupancy?date=2027-07-10"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").get().contains("text/csv"));
        assertTrue(response.body().contains("room,type,reservation,check_in,check_out"));
        assertTrue(response.body().contains("101,STANDARD,2,2027-07-10,2027-07-12"));
        assertTrue(response.body().contains("total,1/5"));
    }

    @Test
    @Order(11)
    void testRevenueReport() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/reports/revenue?month=2027-03"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").get().contains("text/csv"));
        assertTrue(response.body().contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(response.body().contains("total,"));
    }

    private int getMessageCount() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/messages"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(response.body()).size();
    }
}
