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
class TestModeIT {

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
        MessagingService messagingService = new MessagingService(messageRepository, true);
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
        app.start(7071);

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7071;
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
    void shouldRedirectEmailInTestMode() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Test User",
                "guestEmail": "realuser@example.com",
                "guestCpf": "12345678900",
                "checkIn": "2027-04-01",
                "checkOut": "2027-04-02",
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
                .statusCode(201);

        given()
            .when()
                .post("/reservations/1/confirmation")
            .then()
                .statusCode(200);

        given()
            .when()
                .get("/messages")
            .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].to", is("testes@pousadamarealta.com.br"))
                .body("[0].subject", startsWith("[TESTE] "))
                .body("[0].body", containsString("Destinatário original: realuser@example.com"))
                .body("[0].status", is("SENT"));
    }

    @Test
    @Order(2)
    void shouldNotCreateArchiveCopyInTestMode() {
        given()
            .when()
                .get("/messages")
            .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("find { it.to == 'arquivo@pousadamarealta.com.br' }", nullValue());
    }

    @Test
    @Order(3)
    void shouldStillBlockInvalidEmailsInTestMode() {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Invalid Email User",
                "guestEmail": "no-at-sign",
                "guestCpf": "99999999999",
                "checkIn": "2027-05-01",
                "checkOut": "2027-05-02",
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
                .post("/reservations/2/confirmation")
            .then()
                .statusCode(200);

        given()
            .when()
                .get("/messages")
            .then()
                .statusCode(200)
                .body("find { it.to == 'no-at-sign' }.status", is("BLOCKED"));
    }
}
