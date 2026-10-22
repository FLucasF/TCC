package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.config.Database;
import com.pousadamarealta.controller.ApiController;
import com.pousadamarealta.messaging.MessagingService;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationSystemIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static Javalin app;

    @BeforeAll
    static void setUp() {
        Database.initialize(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        RoomService roomService = new RoomService(roomRepository);
        MessagingService messagingService = new MessagingService(messageRepository, false);
        ReservationService reservationService = new ReservationService(
                reservationRepository, roomService, messagingService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
                roomService, reservationService, messagingService, reportService
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        apiController.registerRoutes(app);
        app.start(7070);

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7070;
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
    void shouldGetAllRooms() {
        given()
            .when()
                .get("/rooms")
            .then()
                .statusCode(200)
                .body("size()", is(5))
                .body("[0].number", is("101"))
                .body("[0].type", is("STANDARD"))
                .body("[0].capacity", is(2))
                .body("[0].nightlyRate", is(249.90f));
    }

    @Test
    @Order(2)
    void shouldCreateReservation() {
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

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("roomNumber", is("201"))
                .body("guestName", is("Carla Dias"))
                .body("status", is("PENDING"))
                .body("nights", is(2))
                .body("subtotal", is(779.80f))
                .body("discount", is(0.0f))
                .body("total", is(779.80f))
                .body("amountPaid", is(0.0f));
    }

    @Test
    @Order(3)
    void shouldConfirmReservationAndSendEmail() {
        given()
            .when()
                .post("/reservations/1/confirmation")
            .then()
                .statusCode(200)
                .body("status", is("CONFIRMED"))
                .body("amountPaid", is(779.80f));

        given()
            .when()
                .get("/messages")
            .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].channel", is("EMAIL"))
                .body("[0].to", is("carla@example.com"))
                .body("[0].subject", is("Reserva confirmada - Pousada Maré Alta"))
                .body("[0].status", is("SENT"))
                .body("[1].to", is("arquivo@pousadamarealta.com.br"))
                .body("[1].subject", is("[Cópia] Reserva confirmada - Pousada Maré Alta"))
                .body("[1].status", is("SENT"));
    }

    @Test
    @Order(4)
    void shouldCheckIn() {
        given()
            .when()
                .post("/reservations/1/check-in")
            .then()
                .statusCode(200)
                .body("status", is("CHECKED_IN"));
    }

    @Test
    @Order(5)
    void shouldCheckOut() {
        given()
            .when()
                .post("/reservations/1/check-out")
            .then()
                .statusCode(200)
                .body("title", is("Comprovante de reserva"))
                .body("reservationId", is(1))
                .body("guestName", is("Carla Dias"))
                .body("roomNumber", is("201"))
                .body("roomType", is("DELUXE"));
    }

    @Test
    @Order(6)
    void shouldGetDocument() {
        given()
            .when()
                .get("/reservations/1/document")
            .then()
                .statusCode(200)
                .body("title", is("Comprovante de reserva"))
                .body("reservationId", is(1));
    }

    @Test
    @Order(7)
    void shouldCalculateHighSeasonPricing() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Test User",
                "guestEmail": "test@example.com",
                "guestCpf": "12345678900",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .body("nights", is(2))
                .body("subtotal", is(599.76f))
                .body("total", is(599.76f));
    }

    @Test
    @Order(8)
    void shouldApplyNonRefundableDiscount() {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Discount User",
                "guestEmail": "discount@example.com",
                "guestCpf": "98765432100",
                "checkIn": "2027-03-15",
                "checkOut": "2027-03-17",
                "guests": 1,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .body("subtotal", is(499.80f))
                .body("discount", is(74.97f))
                .body("total", is(424.83f));
    }

    @Test
    @Order(9)
    void shouldBlockInvalidEmail() {
        String requestBody = """
            {
                "roomNumber": "301",
                "guestName": "Invalid Email User",
                "guestEmail": "invalid.email",
                "guestCpf": "11111111111",
                "checkIn": "2027-04-01",
                "checkOut": "2027-04-02",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201);

        given()
            .when()
                .post("/reservations/4/confirmation")
            .then()
                .statusCode(200);

        given()
            .when()
                .get("/messages")
            .then()
                .statusCode(200)
                .body("find { it.to == 'invalid.email' }.status", is("BLOCKED"));
    }

    @Test
    @Order(10)
    void shouldRejectRoomUnavailable() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Conflict User",
                "guestEmail": "conflict@example.com",
                "guestCpf": "55555555555",
                "checkIn": "2027-07-11",
                "checkOut": "2027-07-13",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(409)
                .body("error", is("ROOM_UNAVAILABLE"));
    }

    @Test
    @Order(11)
    void shouldRejectCapacityExceeded() {
        String requestBody = """
            {
                "roomNumber": "201",
                "guestName": "Too Many Guests",
                "guestEmail": "toomany@example.com",
                "guestCpf": "66666666666",
                "checkIn": "2027-05-01",
                "checkOut": "2027-05-02",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(422)
                .body("error", is("CAPACITY_EXCEEDED"));
    }

    @Test
    @Order(12)
    void shouldCancelWithRefund() {
        String requestBody = """
            {
                "roomNumber": "202",
                "guestName": "Cancel User",
                "guestEmail": "cancel@example.com",
                "guestCpf": "77777777777",
                "checkIn": "2027-06-01",
                "checkOut": "2027-06-03",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .extract().path("id");

        given()
            .when()
                .post("/reservations/" + reservationId + "/confirmation")
            .then()
                .statusCode(200);

        given()
            .when()
                .post("/reservations/" + reservationId + "/cancellation")
            .then()
                .statusCode(200)
                .body("status", is("CANCELLED"))
                .body("refund", is(779.80f));
    }

    @Test
    @Order(13)
    void shouldCancelNonRefundableWithoutRefund() {
        String requestBody = """
            {
                "roomNumber": "301",
                "guestName": "No Refund User",
                "guestEmail": "norefund@example.com",
                "guestCpf": "88888888888",
                "checkIn": "2027-08-01",
                "checkOut": "2027-08-05",
                "guests": 3,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .extract().path("id");

        given()
            .when()
                .post("/reservations/" + reservationId + "/confirmation")
            .then()
                .statusCode(200);

        given()
            .when()
                .post("/reservations/" + reservationId + "/cancellation")
            .then()
                .statusCode(200)
                .body("status", is("CANCELLED"))
                .body("refund", is(0.0f));
    }

    @Test
    @Order(14)
    void shouldGenerateOccupancyReport() {
        given()
            .queryParam("date", "2027-03-10")
            .when()
                .get("/reports/occupancy")
            .then()
                .statusCode(200)
                .contentType("text/csv; charset=utf-8")
                .body(containsString("room,type,reservation,check_in,check_out"))
                .body(containsString("201,DELUXE,1,2027-03-10,2027-03-12"))
                .body(containsString("total,1/5"));
    }

    @Test
    @Order(15)
    void shouldGenerateRevenueReport() {
        given()
            .queryParam("month", "2027-03")
            .when()
                .get("/reports/revenue")
            .then()
                .statusCode(200)
                .contentType("text/csv; charset=utf-8")
                .body(containsString("reservation,room,check_in,check_out,status,amount"))
                .body(containsString("1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80"));
    }
}
