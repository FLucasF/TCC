package com.pousadamarealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.controller.ApiController;
import com.pousadamarealta.domain.repository.MessageRepository;
import com.pousadamarealta.domain.repository.ReservationRepository;
import com.pousadamarealta.domain.repository.RoomRepository;
import com.pousadamarealta.infrastructure.DatabaseInitializer;
import com.pousadamarealta.infrastructure.InMemoryRoomRepository;
import com.pousadamarealta.infrastructure.PostgresMessageRepository;
import com.pousadamarealta.infrastructure.PostgresReservationRepository;
import com.pousadamarealta.messaging.MessageService;
import com.pousadamarealta.service.DocumentService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestModeIT {

    private static Javalin app;
    private static DataSource dataSource;

    @BeforeAll
    public static void setup() {
        RestAssured.port = 7072;

        dataSource = createTestDataSource();
        cleanDatabase();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new InMemoryRoomRepository();
        ReservationRepository reservationRepository = new PostgresReservationRepository(dataSource);
        MessageRepository messageRepository = new PostgresMessageRepository(dataSource);

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService(roomRepository);
        MessageService messageService = new MessageService(messageRepository, true);
        ReservationService reservationService = new ReservationService(
            reservationRepository, roomRepository, pricingService, documentService, messageService);
        ReportService reportService = new ReportService(reservationRepository, roomRepository);

        ApiController apiController = new ApiController(
            roomRepository, reservationService, messageService, reportService);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        apiController.registerRoutes(app);
        app.start(7072);
    }

    @AfterAll
    public static void teardown() {
        if (app != null) {
            app.stop();
        }
    }

    @BeforeEach
    public void cleanData() {
        cleanDatabase();
        DatabaseInitializer.initialize(dataSource);
    }

    private static DataSource createTestDataSource() {
        HikariConfig config = new HikariConfig();
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:postgresql://localhost:5432/pousada_test";
        }
        config.setJdbcUrl(dbUrl);
        config.setUsername(System.getenv().getOrDefault("DB_USER", "postgres"));
        config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "postgres"));
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    private static void cleanDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS reservations");
            stmt.execute("DROP TABLE IF EXISTS messages");
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean database", e);
        }
    }

    @Test
    @Order(1)
    public void testModeRedirectsToTestEmail() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Test Guest",
                "guestEmail": "real.guest@example.com",
                "guestCpf": "12345678901",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .post("/reservations/" + reservationId + "/confirmation")
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
            .body("[0].body", startsWith("Destinatário original: real.guest@example.com"))
            .body("[0].status", is("SENT"));
    }

    @Test
    @Order(2)
    public void testModeDoesNotCreateArchiveCopy() {
        String requestBody = """
            {
                "roomNumber": "201",
                "guestName": "Another Test",
                "guestEmail": "another@example.com",
                "guestCpf": "98765432109",
                "checkIn": "2027-04-10",
                "checkOut": "2027-04-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200);

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("size()", is(1));
    }

    @Test
    @Order(3)
    public void testModeStillBlocksInvalidEmails() {
        String requestBody = """
            {
                "roomNumber": "301",
                "guestName": "Invalid Email Test",
                "guestEmail": "invalid.email",
                "guestCpf": "11122233344",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200);

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("size()", is(1))
            .body("[0].to", is("invalid.email"))
            .body("[0].status", is("BLOCKED"));
    }
}
