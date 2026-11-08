package com.pousadamarealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.api.ApiServer;
import com.pousadamarealta.notification.FinanceNotificationListener;
import com.pousadamarealta.notification.GuestNotificationListener;
import com.pousadamarealta.notification.HousekeepingNotificationListener;
import com.pousadamarealta.notification.ReservationEventListener;
import com.pousadamarealta.repository.*;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationSystemIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("pousada_test")
        .withUsername("test")
        .withPassword("test");

    private static Javalin app;
    private static String baseUrl;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        Database database = new Database(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        RoomRepository roomRepository = new PostgresRoomRepository(database);
        ReservationRepository reservationRepository = new PostgresReservationRepository(database);
        MessageRepository messageRepository = new PostgresMessageRepository(database);

        MessageService messageService = new MessageService(messageRepository);

        List<ReservationEventListener> listeners = List.of(
            new GuestNotificationListener(messageService),
            new HousekeepingNotificationListener(messageService),
            new FinanceNotificationListener(messageService)
        );

        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            listeners
        );

        ReportService reportService = new ReportService(reservationService, roomRepository);

        ApiServer apiServer = new ApiServer(
            reservationService,
            roomRepository,
            messageService,
            reportService
        );

        app = apiServer.start(0);
        baseUrl = "http://localhost:" + app.port();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    @Order(1)
    void shouldListRooms() throws Exception {
        HttpResponse<String> response = Unirest.get(baseUrl + "/rooms").asString();

        assertThat(response.getStatus()).isEqualTo(200);

        JsonNode rooms = objectMapper.readTree(response.getBody());
        assertThat(rooms).hasSize(5);

        JsonNode room101 = rooms.get(0);
        assertThat(room101.get("number").asInt()).isEqualTo(101);
        assertThat(room101.get("type").asText()).isEqualTo("STANDARD");
        assertThat(room101.get("capacity").asInt()).isEqualTo(2);
        assertThat(room101.get("nightlyRate").asDouble()).isEqualTo(249.90);
    }

    @Test
    @Order(2)
    void shouldCreateReservation() throws Exception {
        String requestBody = """
            {
                "roomNumber": 201,
                "guestName": "Carla Dias",
                "guestEmail": "carla@example.com",
                "guestCpf": "11122233344",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(requestBody)
            .asString();

        assertThat(response.getStatus()).isEqualTo(201);

        JsonNode reservation = objectMapper.readTree(response.getBody());
        assertThat(reservation.get("id").asLong()).isEqualTo(1);
        assertThat(reservation.get("roomNumber").asInt()).isEqualTo(201);
        assertThat(reservation.get("guestName").asText()).isEqualTo("Carla Dias");
        assertThat(reservation.get("nights").asInt()).isEqualTo(2);
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(779.80);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(0.0);
        assertThat(reservation.get("total").asDouble()).isEqualTo(779.80);
        assertThat(reservation.get("status").asText()).isEqualTo("PENDING");
        assertThat(reservation.get("amountPaid").asDouble()).isEqualTo(0.0);
        assertThat(reservation.get("refund").isNull()).isTrue();
    }

    @Test
    @Order(3)
    void shouldCreateNonRefundableReservationWithDiscount() throws Exception {
        String requestBody = """
            {
                "roomNumber": 101,
                "guestName": "João Silva",
                "guestEmail": "joao@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 2,
                "ratePlan": "NON_REFUNDABLE"
            }
        """;

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(requestBody)
            .asString();

        assertThat(response.getStatus()).isEqualTo(201);

        JsonNode reservation = objectMapper.readTree(response.getBody());
        assertThat(reservation.get("id").asLong()).isEqualTo(2);
        assertThat(reservation.get("nights").asInt()).isEqualTo(2);
        assertThat(reservation.get("subtotal").asDouble()).isEqualTo(599.76);
        assertThat(reservation.get("discount").asDouble()).isEqualTo(89.96);
        assertThat(reservation.get("total").asDouble()).isEqualTo(509.80);
    }

    @Test
    @Order(4)
    void shouldConfirmReservation() throws Exception {
        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/1/confirmation").asString();

        assertThat(response.getStatus()).isEqualTo(200);

        JsonNode reservation = objectMapper.readTree(response.getBody());
        assertThat(reservation.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(reservation.get("amountPaid").asDouble()).isEqualTo(779.80);
    }

    @Test
    @Order(5)
    void shouldSendNotificationsOnConfirmation() throws Exception {
        HttpResponse<String> response = Unirest.get(baseUrl + "/messages").asString();

        assertThat(response.getStatus()).isEqualTo(200);

        JsonNode messages = objectMapper.readTree(response.getBody());
        assertThat(messages).hasSize(3);

        JsonNode guestMessage = messages.get(0);
        assertThat(guestMessage.get("channel").asText()).isEqualTo("EMAIL");
        assertThat(guestMessage.get("to").asText()).isEqualTo("carla@example.com");
        assertThat(guestMessage.get("subject").asText()).isEqualTo("Reserva confirmada - Pousada Maré Alta");
        assertThat(guestMessage.get("body").asText()).contains("Carla Dias");
        assertThat(guestMessage.get("body").asText()).contains("Comprovante de reserva nº 1");

        JsonNode housekeepingMessage = messages.get(1);
        assertThat(housekeepingMessage.get("to").asText()).isEqualTo("governanca@pousadamarealta.com.br");
        assertThat(housekeepingMessage.get("subject").asText()).isEqualTo("Reserva confirmada - quarto 201");
        assertThat(housekeepingMessage.get("body").asText()).contains("2 hóspedes");

        JsonNode financeMessage = messages.get(2);
        assertThat(financeMessage.get("to").asText()).isEqualTo("financeiro@pousadamarealta.com.br");
        assertThat(financeMessage.get("subject").asText()).isEqualTo("Pagamento recebido - reserva 1");
        assertThat(financeMessage.get("body").asText()).contains("R$ 779,80");
    }

    @Test
    @Order(6)
    void shouldCheckIn() throws Exception {
        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/1/check-in").asString();

        assertThat(response.getStatus()).isEqualTo(200);

        JsonNode reservation = objectMapper.readTree(response.getBody());
        assertThat(reservation.get("status").asText()).isEqualTo("CHECKED_IN");
    }

    @Test
    @Order(7)
    void shouldCheckOut() throws Exception {
        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/1/check-out").asString();

        assertThat(response.getStatus()).isEqualTo(200);

        JsonNode document = objectMapper.readTree(response.getBody());
        assertThat(document.get("title").asText()).isEqualTo("Comprovante de reserva");
        assertThat(document.get("reservationId").asLong()).isEqualTo(1);
        assertThat(document.get("guestName").asText()).isEqualTo("Carla Dias");
    }

    @Test
    @Order(8)
    void shouldGetDocument() throws Exception {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reservations/1/document").asString();

        assertThat(response.getStatus()).isEqualTo(200);

        JsonNode document = objectMapper.readTree(response.getBody());
        assertThat(document.get("title").asText()).isEqualTo("Comprovante de reserva");
    }

    @Test
    @Order(9)
    void shouldCancelConfirmedReservation() throws Exception {
        HttpResponse<String> confirmResponse = Unirest.post(baseUrl + "/reservations/2/confirmation").asString();
        assertThat(confirmResponse.getStatus()).isEqualTo(200);

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/2/cancellation").asString();

        assertThat(response.getStatus()).isEqualTo(200);

        JsonNode reservation = objectMapper.readTree(response.getBody());
        assertThat(reservation.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(reservation.get("refund").asDouble()).isEqualTo(0.0);
    }

    @Test
    @Order(10)
    void shouldSendNotificationsOnCancellation() throws Exception {
        HttpResponse<String> response = Unirest.get(baseUrl + "/messages").asString();
        JsonNode messages = objectMapper.readTree(response.getBody());

        boolean foundHousekeepingCancellation = false;
        boolean foundFinanceCancellation = false;

        for (JsonNode msg : messages) {
            String subject = msg.get("subject").asText();
            if (subject.equals("Reserva cancelada - quarto 101")) {
                foundHousekeepingCancellation = true;
                assertThat(msg.get("to").asText()).isEqualTo("governanca@pousadamarealta.com.br");
            }
        }

        assertThat(foundHousekeepingCancellation).isTrue();
        assertThat(foundFinanceCancellation).isFalse();
    }

    @Test
    @Order(11)
    void shouldRejectRoomNotFound() throws Exception {
        String requestBody = """
            {
                "roomNumber": 999,
                "guestName": "Test",
                "guestEmail": "test@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(requestBody)
            .asString();

        assertThat(response.getStatus()).isEqualTo(404);

        JsonNode error = objectMapper.readTree(response.getBody());
        assertThat(error.get("error").asText()).isEqualTo("ROOM_NOT_FOUND");
    }

    @Test
    @Order(12)
    void shouldRejectCapacityExceeded() throws Exception {
        String requestBody = """
            {
                "roomNumber": 101,
                "guestName": "Test",
                "guestEmail": "test@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-04-10",
                "checkOut": "2027-04-12",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(requestBody)
            .asString();

        assertThat(response.getStatus()).isEqualTo(422);

        JsonNode error = objectMapper.readTree(response.getBody());
        assertThat(error.get("error").asText()).isEqualTo("CAPACITY_EXCEEDED");
    }

    @Test
    @Order(13)
    void shouldGenerateOccupancyReport() throws Exception {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reports/occupancy?date=2027-03-10").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Type")).contains("text/csv");

        String csv = response.getBody();
        String[] lines = csv.split("\n");

        assertThat(lines[0]).isEqualTo("room,type,reservation,check_in,check_out");
        assertThat(lines[1]).isEqualTo("201,DELUXE,1,2027-03-10,2027-03-12");
        assertThat(lines[2]).isEqualTo("total,1/5");
    }

    @Test
    @Order(14)
    void shouldGenerateRevenueReport() throws Exception {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reports/revenue?month=2027-03").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Type")).contains("text/csv");

        String csv = response.getBody();
        String[] lines = csv.split("\n");

        assertThat(lines[0]).isEqualTo("reservation,room,check_in,check_out,status,amount");
        assertThat(lines[1]).isEqualTo("1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80");
        assertThat(lines[2]).isEqualTo("total,779.80");
    }
}
