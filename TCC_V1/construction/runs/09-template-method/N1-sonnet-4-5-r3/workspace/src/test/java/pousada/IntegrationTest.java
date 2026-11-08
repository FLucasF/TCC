package pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pousada.config.Database;
import pousada.controller.MessageController;
import pousada.controller.ReportController;
import pousada.controller.ReservationController;
import pousada.controller.RoomController;
import pousada.repository.MessageRepository;
import pousada.repository.ReservationRepository;
import pousada.repository.RoomRepository;
import pousada.service.MessageService;
import pousada.service.ReservationService;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("pousada_test")
        .withUsername("test")
        .withPassword("test");

    static Javalin app;
    static String baseUrl = "http://localhost:7071";

    @BeforeAll
    static void setUp() {
        Database.initialize(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        RoomRepository roomRepository = new RoomRepository();
        roomRepository.initializeRooms();

        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        MessageService messageService = new MessageService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            messageService
        );

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(reservationService, roomRepository);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reservationRepository, roomRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

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
        app.get("/reports/tourism-tax", reportController::getTourismTaxReport);
        app.get("/reports/rate-plans", reportController::getRatePlanReport);

        app.start(7071);
    }

    @AfterAll
    static void tearDown() {
        if (app != null) {
            app.stop();
        }
        Database.close();
    }

    @Test
    @Order(1)
    void testGetRooms() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/rooms").asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("101", "102", "201", "202", "301");
    }

    @Test
    @Order(2)
    void testCreateReservation() {
        String body = """
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

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(body)
            .asString();

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getBody()).contains("\"id\":1");
        assertThat(response.getBody()).contains("\"status\":\"PENDING\"");
        assertThat(response.getBody()).contains("\"subtotal\":599.76");
        assertThat(response.getBody()).contains("\"total\":599.76");
    }

    @Test
    @Order(3)
    void testConfirmReservation() {
        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/1/confirmation")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"CONFIRMED\"");
        assertThat(response.getBody()).contains("\"amountPaid\":599.76");
    }

    @Test
    @Order(4)
    void testEmailWasSent() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/messages").asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"channel\":\"EMAIL\"");
        assertThat(response.getBody()).contains("\"to\":\"joao@example.com\"");
        assertThat(response.getBody()).contains("Reserva confirmada - Pousada Maré Alta");
        assertThat(response.getBody()).contains("Comprovante de reserva nº 1");
    }

    @Test
    @Order(5)
    void testCheckIn() {
        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/1/check-in")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"CHECKED_IN\"");
    }

    @Test
    @Order(6)
    void testCheckOut() {
        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/1/check-out")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"title\":\"Comprovante de reserva\"");
        assertThat(response.getBody()).contains("\"reservationId\":1");
    }

    @Test
    @Order(7)
    void testGetDocument() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reservations/1/document")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"title\":\"Comprovante de reserva\"");
    }

    @Test
    @Order(8)
    void testNonRefundableReservation() {
        String body = """
            {
                "roomNumber": "201",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "98765432100",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "NON_REFUNDABLE"
            }
        """;

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(body)
            .asString();

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getBody()).contains("\"subtotal\":779.80");
        assertThat(response.getBody()).contains("\"discount\":116.97");
        assertThat(response.getBody()).contains("\"total\":662.83");
    }

    @Test
    @Order(9)
    void testRoomUnavailable() {
        String body = """
            {
                "roomNumber": "101",
                "guestName": "Pedro Costa",
                "guestEmail": "pedro@example.com",
                "guestCpf": "11122233344",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(body)
            .asString();

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getBody()).contains("\"error\":\"ROOM_UNAVAILABLE\"");
    }

    @Test
    @Order(10)
    void testCapacityExceeded() {
        String body = """
            {
                "roomNumber": "102",
                "guestName": "Ana Oliveira",
                "guestEmail": "ana@example.com",
                "guestCpf": "55566677788",
                "checkIn": "2027-08-01",
                "checkOut": "2027-08-03",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
        """;

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(body)
            .asString();

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(response.getBody()).contains("\"error\":\"CAPACITY_EXCEEDED\"");
    }

    @Test
    @Order(11)
    void testOccupancyReport() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reports/occupancy")
            .queryString("date", "2027-07-10")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Pousada Maré Alta,occupancy,2027-07-10");
        assertThat(response.getBody()).contains("room,type,reservation,check_in,check_out");
        assertThat(response.getBody()).contains("101,STANDARD,1,2027-07-10,2027-07-12");
        assertThat(response.getBody()).contains("total,1/5");
    }

    @Test
    @Order(12)
    void testRevenueReport() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reports/revenue")
            .queryString("month", "2027-07")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Pousada Maré Alta,revenue,2027-07");
        assertThat(response.getBody()).contains("reservation,room,check_in,check_out,status,amount");
        assertThat(response.getBody()).contains("1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76");
        assertThat(response.getBody()).contains("total,599.76");
    }

    @Test
    @Order(13)
    void testTourismTaxReport() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reports/tourism-tax")
            .queryString("month", "2027-07")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Pousada Maré Alta,tourism-tax,2027-07");
        assertThat(response.getBody()).contains("reservation,room,check_in,check_out,guests,nights,tax");
        assertThat(response.getBody()).contains("1,101,2027-07-10,2027-07-12,2,2,18.00");
        assertThat(response.getBody()).contains("total,18.00");
    }

    @Test
    @Order(14)
    void testRatePlanReport() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/reports/rate-plans")
            .queryString("month", "2027-07")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Pousada Maré Alta,rate-plans,2027-07");
        assertThat(response.getBody()).contains("rate_plan,reservations,amount");
        assertThat(response.getBody()).contains("STANDARD,1,599.76");
        assertThat(response.getBody()).contains("NON_REFUNDABLE,0,0");
        assertThat(response.getBody()).contains("total,1,599.76");
    }

    @Test
    @Order(15)
    void testCancelNonRefundable() {
        Unirest.post(baseUrl + "/reservations/2/confirmation").asString();

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/2/cancellation")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"CANCELLED\"");
        assertThat(response.getBody()).contains("\"refund\":0");
    }

    @Test
    @Order(16)
    void testCancelStandardPlan() {
        String body = """
            {
                "roomNumber": "202",
                "guestName": "Carlos Lima",
                "guestEmail": "carlos@example.com",
                "guestCpf": "33344455566",
                "checkIn": "2027-08-15",
                "checkOut": "2027-08-17",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
        """;

        Unirest.post(baseUrl + "/reservations")
            .header("Content-Type", "application/json")
            .body(body)
            .asString();

        Unirest.post(baseUrl + "/reservations/3/confirmation").asString();

        HttpResponse<String> response = Unirest.post(baseUrl + "/reservations/3/cancellation")
            .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"CANCELLED\"");
        assertThat(response.getBody()).contains("\"refund\":779.80");
    }
}
