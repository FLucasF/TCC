package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.controller.MessageController;
import com.pousadamarealta.api.controller.ReportController;
import com.pousadamarealta.api.controller.ReservationController;
import com.pousadamarealta.api.controller.RoomController;
import com.pousadamarealta.api.dto.MessageDTO;
import com.pousadamarealta.domain.listener.FinanceNotificationListener;
import com.pousadamarealta.domain.listener.GuestNotificationListener;
import com.pousadamarealta.domain.listener.HousekeepingNotificationListener;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.domain.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationTest {
    private static Javalin app;
    private static final int TEST_PORT = 17070;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        PricingService pricingService = new PricingService();
        MessageService messageService = new MessageService(messageRepository);
        EventBus eventBus = new EventBus();

        eventBus.subscribe(new GuestNotificationListener(messageService));
        eventBus.subscribe(new HousekeepingNotificationListener(messageService));
        eventBus.subscribe(new FinanceNotificationListener(messageService));

        RoomService roomService = new RoomService(roomRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository, pricingService, eventBus
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(
            reservationService, roomService
        );
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(TEST_PORT);

        app.get("/rooms", roomController::getRooms);
        app.post("/reservations", reservationController::createReservation);
        app.get("/reservations/{id}", reservationController::getReservation);
        app.post("/reservations/{id}/confirmation", reservationController::confirmReservation);
        app.post("/reservations/{id}/cancellation", reservationController::cancelReservation);
        app.post("/reservations/{id}/check-in", reservationController::checkInReservation);
        app.post("/reservations/{id}/check-out", reservationController::checkOutReservation);
        app.get("/reservations/{id}/document", reservationController::getDocument);
        app.get("/messages", messageController::getMessages);
        app.get("/reports/occupancy", reportController::getOccupancyReport);
        app.get("/reports/revenue", reportController::getRevenueReport);
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    void shouldCompleteFullReservationFlow() throws Exception {
        String createBody = """
            {
                "roomNumber": "201",
                "guestName": "Carla Dias",
                "guestEmail": "carla@email.com",
                "guestCpf": "11122233344",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createBody))
            .build();

        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createResponse.statusCode()).isEqualTo(201);
        assertThat(createResponse.body()).contains("\"status\":\"PENDING\"");

        String reservationId = createResponse.body().split("\"id\":")[1].split(",")[0];

        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/" + reservationId + "/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> confirmResponse = httpClient.send(confirmRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(confirmResponse.statusCode()).isEqualTo(200);
        assertThat(confirmResponse.body()).contains("\"status\":\"CONFIRMED\"");

        HttpRequest messagesRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesResponse = httpClient.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(messagesResponse.statusCode()).isEqualTo(200);
        assertThat(messagesResponse.body()).contains("carla@email.com");
        assertThat(messagesResponse.body()).contains("Reserva confirmada - Pousada Maré Alta");
        assertThat(messagesResponse.body()).contains("governanca@pousadamarealta.com.br");
        assertThat(messagesResponse.body()).contains("Reserva confirmada - quarto 201");
        assertThat(messagesResponse.body()).contains("financeiro@pousadamarealta.com.br");
        assertThat(messagesResponse.body()).contains("Pagamento recebido - reserva " + reservationId);
    }

    @Test
    void shouldHandleCancellationWithNotifications() throws Exception {
        String createBody = """
            {
                "roomNumber": "202",
                "guestName": "João Silva",
                "guestEmail": "joao@email.com",
                "guestCpf": "99988877766",
                "checkIn": "2027-04-10",
                "checkOut": "2027-04-15",
                "guests": 3,
                "ratePlan": "STANDARD"
            }
            """;

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createBody))
            .build();

        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createResponse.statusCode()).isEqualTo(201);

        String reservationId = createResponse.body().split("\"id\":")[1].split(",")[0];

        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/" + reservationId + "/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        httpClient.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest cancelRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/" + reservationId + "/cancellation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> cancelResponse = httpClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(cancelResponse.statusCode()).isEqualTo(200);
        assertThat(cancelResponse.body()).contains("\"status\":\"CANCELLED\"");

        HttpRequest messagesRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesResponse = httpClient.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(messagesResponse.body()).contains("Reserva cancelada - quarto 202");
        assertThat(messagesResponse.body()).contains("Reembolso - reserva " + reservationId);
    }

    @Test
    void shouldNotNotifyFinanceWhenNoRefund() throws Exception {
        String createBody = """
            {
                "roomNumber": "101",
                "guestName": "Maria Santos",
                "guestEmail": "maria@email.com",
                "guestCpf": "55544433322",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-12",
                "guests": 2,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(createBody))
            .build();

        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        String reservationId = createResponse.body().split("\"id\":")[1].split(",")[0];

        HttpRequest confirmRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/" + reservationId + "/confirmation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        httpClient.send(confirmRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest messagesBeforeCancelRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesBeforeCancel = httpClient.send(messagesBeforeCancelRequest,
            HttpResponse.BodyHandlers.ofString());
        int messagesCountBefore = messagesBeforeCancel.body().split("\"id\":").length - 1;

        HttpRequest cancelRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/reservations/" + reservationId + "/cancellation"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        httpClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());

        HttpRequest messagesAfterCancelRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/messages"))
            .GET()
            .build();

        HttpResponse<String> messagesAfterCancel = httpClient.send(messagesAfterCancelRequest,
            HttpResponse.BodyHandlers.ofString());
        int messagesCountAfter = messagesAfterCancel.body().split("\"id\":").length - 1;

        assertThat(messagesCountAfter - messagesCountBefore).isEqualTo(1);
        assertThat(messagesAfterCancel.body()).contains("Reserva cancelada - quarto 101");
        assertThat(messagesAfterCancel.body()).doesNotContain("Reembolso - reserva " + reservationId);
    }
}
