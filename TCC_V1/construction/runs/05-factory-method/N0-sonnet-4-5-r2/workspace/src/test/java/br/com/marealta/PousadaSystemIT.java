package br.com.marealta;

import br.com.marealta.controller.*;
import br.com.marealta.database.DatabaseInitializer;
import br.com.marealta.dto.ErrorResponse;
import br.com.marealta.exception.PousadaException;
import br.com.marealta.repository.MessageRepository;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;
import br.com.marealta.service.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PousadaSystemIT {

    private static Javalin app;
    private static final int TEST_PORT = 7777;

    @BeforeAll
    public static void setup() {
        DataSource dataSource = createTestDataSource();

        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new RoomRepository(dataSource);
        ReservationRepository reservationRepository = new ReservationRepository(dataSource);
        MessageRepository messageRepository = new MessageRepository(dataSource);

        roomRepository.initializeRooms();

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService(pricingService);
        EmailService emailService = new EmailService(messageRepository);
        ReservationService reservationService = new ReservationService(
            reservationRepository,
            roomRepository,
            pricingService,
            documentService,
            emailService
        );
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        RoomController roomController = new RoomController(roomRepository);
        ReservationController reservationController = new ReservationController(
            reservationService,
            roomRepository,
            documentService
        );
        MessageController messageController = new MessageController(messageRepository);
        ReportController reportController = new ReportController(reportService);

        app = Javalin.create(config -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            config.jsonMapper(new JavalinJackson(objectMapper, false));
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

        app.exception(PousadaException.class, (e, ctx) -> {
            ctx.status(e.getHttpStatus())
               .json(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse("INTERNAL_ERROR", "Internal server error"));
        });

        app.start(TEST_PORT);

        RestAssured.port = TEST_PORT;
        RestAssured.baseURI = "http://localhost";
    }

    @AfterAll
    public static void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    @Test
    public void shouldListRooms() {
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
    public void shouldCreateReservationWithCpf() {
        String requestBody = """
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
            .body(requestBody)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("roomNumber", is("101"))
            .body("guestName", is("João Silva"))
            .body("guestCpf", is("12345678901"))
            .body("nights", is(2))
            .body("subtotal", is(499.80f))
            .body("discount", is(0.0f))
            .body("total", is(499.80f))
            .body("status", is("PENDING"))
            .body("amountPaid", is(0.0f));
    }

    @Test
    public void shouldCalculateHighSeasonPricing() {
        String requestBody = """
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
            .body(requestBody)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("subtotal", is(599.76f));
    }

    @Test
    public void shouldApplyNonRefundableDiscount() {
        String requestBody = """
            {
                "roomNumber": "201",
                "guestName": "Pedro Oliveira",
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
            .body(requestBody)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("subtotal", is(779.80f))
            .body("discount", is(116.97f))
            .body("total", is(662.83f));
    }

    @Test
    public void shouldConfirmReservationAndSendEmail() {
        String createRequest = """
            {
                "roomNumber": "102",
                "guestName": "Ana Costa",
                "guestEmail": "ana@example.com",
                "guestCpf": "55566677788",
                "checkIn": "2027-04-15",
                "checkOut": "2027-04-16",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(createRequest)
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
            .body("amountPaid", is(249.90f));

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[-1].channel", is("EMAIL"))
            .body("[-1].to", is("ana@example.com"))
            .body("[-1].subject", is("Reserva confirmada - Pousada Maré Alta"))
            .body("[-1].body", containsString("Olá, Ana Costa!"))
            .body("[-1].body", containsString("Comprovante de reserva"));
    }

    @Test
    public void shouldRejectRoomUnavailableConflict() {
        String request1 = """
            {
                "roomNumber": "301",
                "guestName": "Carlos Lima",
                "guestEmail": "carlos@example.com",
                "guestCpf": "99988877766",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int id = given()
            .contentType(ContentType.JSON)
            .body(request1)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .when()
            .post("/reservations/" + id + "/confirmation")
            .then()
            .statusCode(200);

        String request2 = """
            {
                "roomNumber": "301",
                "guestName": "Fernanda Souza",
                "guestEmail": "fernanda@example.com",
                "guestCpf": "11111111111",
                "checkIn": "2027-05-11",
                "checkOut": "2027-05-13",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(request2)
            .when()
            .post("/reservations")
            .then()
            .statusCode(409)
            .body("error", is("ROOM_UNAVAILABLE"));
    }

    @Test
    public void shouldRejectCapacityExceeded() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Grupo Grande",
                "guestEmail": "grupo@example.com",
                "guestCpf": "22233344455",
                "checkIn": "2027-06-01",
                "checkOut": "2027-06-02",
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

    private static DataSource createTestDataSource() {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/pousada_test");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(5);

        return new HikariDataSource(config);
    }
}
