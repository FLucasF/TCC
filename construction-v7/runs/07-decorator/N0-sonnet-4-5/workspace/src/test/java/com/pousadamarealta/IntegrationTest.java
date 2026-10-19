package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.MessageController;
import com.pousadamarealta.controller.ReportController;
import com.pousadamarealta.controller.ReservationController;
import com.pousadamarealta.controller.RoomController;
import com.pousadamarealta.dto.ErrorResponse;
import com.pousadamarealta.model.ApiException;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.json.JavalinJackson;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class IntegrationTest {
    private static Javalin app;
    private static final int TEST_PORT = 7071;

    @BeforeAll
    public static void setUp() {
        RestAssured.port = TEST_PORT;
        RestAssured.baseURI = "http://localhost";

        System.clearProperty("HOTEL_TEST_MODE");

        MessageService messageService = new MessageService();
        RoomService roomService = new RoomService();
        ReservationService reservationService = new ReservationService(roomService, messageService);
        ReportService reportService = new ReportService(reservationService, roomService);

        RoomController roomController = new RoomController(roomService);
        ReservationController reservationController = new ReservationController(reservationService, roomService);
        MessageController messageController = new MessageController(messageService);
        ReportController reportController = new ReportController(reportService);

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

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(BadRequestResponse.class, (e, ctx) -> {
            ctx.status(400);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Corpo da requisição inválido"));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "Erro interno do servidor"));
        });

        app.start(TEST_PORT);
    }

    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    public void testGetRooms() {
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
    public void testCreateReservationStandardPlan() {
        String body = """
            {
                "roomNumber": "101",
                "guestName": "João Silva",
                "guestEmail": "joao@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("status", is("PENDING"))
            .body("nights", is(2))
            .body("subtotal", is(499.80f))
            .body("discount", is(0))
            .body("total", is(499.80f))
            .body("amountPaid", is(0));
    }

    @Test
    public void testCreateReservationHighSeason() {
        String body = """
            {
                "roomNumber": "101",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "98765432109",
                "checkIn": "2027-07-10",
                "checkOut": "2027-07-12",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("nights", is(2))
            .body("subtotal", is(599.76f))
            .body("discount", is(0))
            .body("total", is(599.76f));
    }

    @Test
    public void testCreateReservationNonRefundable() {
        String body = """
            {
                "roomNumber": "201",
                "guestName": "Pedro Costa",
                "guestEmail": "pedro@example.com",
                "guestCpf": "11122233344",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("subtotal", is(779.80f))
            .body("discount", is(116.97f))
            .body("total", is(662.83f));
    }

    @Test
    public void testConfirmReservationSendsEmail() {
        String body = """
            {
                "roomNumber": "102",
                "guestName": "Carla Dias",
                "guestEmail": "carla@example.com",
                "guestCpf": "11122233344",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200)
            .body("status", is("CONFIRMED"))
            .body("amountPaid", is(499.80f));

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(2))
            .body("findAll { it.to == 'carla@example.com' }[0].status", is("SENT"))
            .body("findAll { it.to == 'carla@example.com' }[0].subject", is("Reserva confirmada - Pousada Maré Alta"))
            .body("findAll { it.to == 'carla@example.com' }[0].body", containsString("Olá, Carla Dias!"))
            .body("findAll { it.to == 'carla@example.com' }[0].body", containsString("Comprovante de reserva nº"))
            .body("findAll { it.to == 'arquivo@pousadamarealta.com.br' }[0].status", is("SENT"))
            .body("findAll { it.to == 'arquivo@pousadamarealta.com.br' }[0].subject", is("[Cópia] Reserva confirmada - Pousada Maré Alta"));
    }

    @Test
    public void testInvalidEmailBlocked() {
        String body = """
            {
                "roomNumber": "301",
                "guestName": "Ana Paula",
                "guestEmail": "ana.gmail.com",
                "guestCpf": "99988877766",
                "checkIn": "2027-04-01",
                "checkOut": "2027-04-03",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200);

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("findAll { it.to == 'ana.gmail.com' }[0].status", is("BLOCKED"));
    }

    @Test
    public void testRoomUnavailable() {
        String body1 = """
            {
                "roomNumber": "202",
                "guestName": "Guest 1",
                "guestEmail": "guest1@example.com",
                "guestCpf": "11111111111",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-15",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body1)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201);

        String body2 = """
            {
                "roomNumber": "202",
                "guestName": "Guest 2",
                "guestEmail": "guest2@example.com",
                "guestCpf": "22222222222",
                "checkIn": "2027-05-12",
                "checkOut": "2027-05-14",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body2)
            .when()
            .post("/reservations")
            .then()
            .statusCode(409)
            .body("error", is("ROOM_UNAVAILABLE"));
    }

    @Test
    public void testCapacityExceeded() {
        String body = """
            {
                "roomNumber": "101",
                "guestName": "Big Family",
                "guestEmail": "family@example.com",
                "guestCpf": "33333333333",
                "checkIn": "2027-06-01",
                "checkOut": "2027-06-02",
                "guests": 5,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(422)
            .body("error", is("CAPACITY_EXCEEDED"));
    }

    @Test
    public void testCheckInCheckOut() {
        String body = """
            {
                "roomNumber": "301",
                "guestName": "Test Guest",
                "guestEmail": "test@example.com",
                "guestCpf": "44444444444",
                "checkIn": "2027-08-01",
                "checkOut": "2027-08-03",
                "guests": 3,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200);

        given()
            .when()
            .post("/reservations/" + reservationId + "/check-in")
            .then()
            .statusCode(200)
            .body("status", is("CHECKED_IN"));

        given()
            .when()
            .post("/reservations/" + reservationId + "/check-out")
            .then()
            .statusCode(200)
            .body("title", is("Comprovante de reserva"))
            .body("reservationId", is(reservationId))
            .body("guestName", is("Test Guest"));

        given()
            .when()
            .get("/reservations/" + reservationId)
            .then()
            .statusCode(200)
            .body("status", is("CHECKED_OUT"));
    }

    @Test
    public void testCancellationStandard() {
        String body = """
            {
                "roomNumber": "201",
                "guestName": "Cancel Test",
                "guestEmail": "cancel@example.com",
                "guestCpf": "55555555555",
                "checkIn": "2027-09-01",
                "checkOut": "2027-09-03",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200)
            .body("amountPaid", is(779.80f));

        given()
            .when()
            .post("/reservations/" + reservationId + "/cancellation")
            .then()
            .statusCode(200)
            .body("status", is("CANCELLED"))
            .body("refund", is(779.80f));
    }

    @Test
    public void testCancellationNonRefundable() {
        String body = """
            {
                "roomNumber": "202",
                "guestName": "Non Refund Cancel",
                "guestEmail": "nrc@example.com",
                "guestCpf": "66666666666",
                "checkIn": "2027-10-01",
                "checkOut": "2027-10-03",
                "guests": 2,
                "ratePlan": "NON_REFUNDABLE"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200)
            .body("amountPaid", is(662.83f));

        given()
            .when()
            .post("/reservations/" + reservationId + "/cancellation")
            .then()
            .statusCode(200)
            .body("status", is("CANCELLED"))
            .body("refund", is(0));
    }

    @Test
    public void testOccupancyReport() {
        given()
            .queryParam("date", "2027-05-12")
            .when()
            .get("/reports/occupancy")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .body(containsString("room,type,reservation,check_in,check_out"))
            .body(containsString("total,"));
    }

    @Test
    public void testRevenueReport() {
        given()
            .queryParam("month", "2027-03")
            .when()
            .get("/reports/revenue")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .body(containsString("reservation,room,check_in,check_out,status,amount"))
            .body(containsString("total,"));
    }

    @Test
    public void testInvalidDateFormat() {
        String body = """
            {
                "roomNumber": "101",
                "guestName": "Test",
                "guestEmail": "test@example.com",
                "guestCpf": "77777777777",
                "checkIn": "10/03/2027",
                "checkOut": "12/03/2027",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/reservations")
            .then()
            .statusCode(400)
            .body("error", is("INVALID_REQUEST"));
    }

    @Test
    public void testReservationNotFound() {
        given()
            .when()
            .get("/reservations/99999")
            .then()
            .statusCode(404)
            .body("error", is("RESERVATION_NOT_FOUND"));
    }
}
