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
public class ReservationSystemIT {

    private static Javalin app;
    private static DataSource dataSource;

    @BeforeAll
    public static void setup() {
        RestAssured.port = 7071;

        dataSource = createTestDataSource();
        cleanDatabase();
        DatabaseInitializer.initialize(dataSource);

        RoomRepository roomRepository = new InMemoryRoomRepository();
        ReservationRepository reservationRepository = new PostgresReservationRepository(dataSource);
        MessageRepository messageRepository = new PostgresMessageRepository(dataSource);

        PricingService pricingService = new PricingService();
        DocumentService documentService = new DocumentService(roomRepository);
        MessageService messageService = new MessageService(messageRepository, false);
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
        app.start(7071);
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
    @Order(2)
    public void testCreateReservationLowSeason() {
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
            .body("nights", is(2))
            .body("subtotal", is(499.80f))
            .body("discount", is(0.00f))
            .body("total", is(499.80f))
            .body("status", is("PENDING"))
            .body("amountPaid", is(0.00f));
    }

    @Test
    @Order(3)
    public void testCreateReservationHighSeason() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "98765432109",
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
            .body("subtotal", is(599.76f))
            .body("total", is(599.76f));
    }

    @Test
    @Order(4)
    public void testCreateReservationNonRefundable() {
        String requestBody = """
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
    @Order(5)
    public void testRoomUnavailable() {
        String request1 = """
            {
                "roomNumber": "101",
                "guestName": "Ana Silva",
                "guestEmail": "ana@example.com",
                "guestCpf": "11111111111",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-15",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(request1)
            .post("/reservations")
            .then()
            .statusCode(201);

        String request2 = """
            {
                "roomNumber": "101",
                "guestName": "Carlos Souza",
                "guestEmail": "carlos@example.com",
                "guestCpf": "22222222222",
                "checkIn": "2027-05-12",
                "checkOut": "2027-05-17",
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
    @Order(6)
    public void testCapacityExceeded() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Família Grande",
                "guestEmail": "familia@example.com",
                "guestCpf": "33333333333",
                "checkIn": "2027-06-10",
                "checkOut": "2027-06-12",
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
    @Order(7)
    public void testConfirmReservationSendsEmail() {
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

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
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
            .body("amountPaid", is(779.80f));

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("size()", is(2))
            .body("[0].to", is("carla@example.com"))
            .body("[0].subject", is("Reserva confirmada - Pousada Maré Alta"))
            .body("[0].status", is("SENT"))
            .body("[0].body", containsString("Olá, Carla Dias!"))
            .body("[1].to", is("arquivo@pousadamarealta.com.br"))
            .body("[1].subject", is("[Cópia] Reserva confirmada - Pousada Maré Alta"))
            .body("[1].status", is("SENT"));
    }

    @Test
    @Order(8)
    public void testEmailValidation() {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Email Inválido",
                "guestEmail": "ana@gmail",
                "guestCpf": "44455566677",
                "checkIn": "2027-04-10",
                "checkOut": "2027-04-12",
                "guests": 1,
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
            .body("[0].to", is("ana@gmail"))
            .body("[0].status", is("BLOCKED"));
    }

    @Test
    @Order(9)
    public void testCancelReservationStandard() {
        String requestBody = """
            {
                "roomNumber": "301",
                "guestName": "Cancelador Standard",
                "guestEmail": "cancel@example.com",
                "guestCpf": "55566677788",
                "checkIn": "2027-08-10",
                "checkOut": "2027-08-12",
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
            .post("/reservations/" + reservationId + "/cancellation")
            .then()
            .statusCode(200)
            .body("status", is("CANCELLED"))
            .body("refund", is(1391.76f));
    }

    @Test
    @Order(10)
    public void testCheckInAndCheckOut() {
        String requestBody = """
            {
                "roomNumber": "202",
                "guestName": "Hospede Completo",
                "guestEmail": "hospede@example.com",
                "guestCpf": "66677788899",
                "checkIn": "2027-05-20",
                "checkOut": "2027-05-22",
                "guests": 3,
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

        given().post("/reservations/" + reservationId + "/confirmation").then().statusCode(200);

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
            .body("guestName", is("Hospede Completo"));
    }

    @Test
    @Order(11)
    public void testOccupancyReport() {
        cleanData();
        DatabaseInitializer.initialize(dataSource);

        String request1 = """
            {
                "roomNumber": "101",
                "guestName": "Guest 1",
                "guestEmail": "guest1@example.com",
                "guestCpf": "11111111111",
                "checkIn": "2027-06-10",
                "checkOut": "2027-06-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int id1 = given().contentType(ContentType.JSON).body(request1).post("/reservations")
            .then().statusCode(201).extract().path("id");
        given().post("/reservations/" + id1 + "/confirmation").then().statusCode(200);

        String request2 = """
            {
                "roomNumber": "201",
                "guestName": "Guest 2",
                "guestEmail": "guest2@example.com",
                "guestCpf": "22222222222",
                "checkIn": "2027-06-09",
                "checkOut": "2027-06-11",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int id2 = given().contentType(ContentType.JSON).body(request2).post("/reservations")
            .then().statusCode(201).extract().path("id");
        given().post("/reservations/" + id2 + "/confirmation").then().statusCode(200);

        String csv = given()
            .queryParam("date", "2027-06-10")
            .when()
            .get("/reports/occupancy")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .extract()
            .asString();

        Assertions.assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        Assertions.assertTrue(csv.contains("101,STANDARD," + id1 + ",2027-06-10,2027-06-12"));
        Assertions.assertTrue(csv.contains("201,DELUXE," + id2 + ",2027-06-09,2027-06-11"));
        Assertions.assertTrue(csv.contains("total,2/5"));
    }

    @Test
    @Order(12)
    public void testRevenueReport() {
        cleanData();
        DatabaseInitializer.initialize(dataSource);

        String request1 = """
            {
                "roomNumber": "101",
                "guestName": "Revenue Guest 1",
                "guestEmail": "revenue1@example.com",
                "guestCpf": "77788899900",
                "checkIn": "2027-09-10",
                "checkOut": "2027-09-12",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int id1 = given().contentType(ContentType.JSON).body(request1).post("/reservations")
            .then().statusCode(201).extract().path("id");
        given().post("/reservations/" + id1 + "/confirmation").then().statusCode(200);

        String csv = given()
            .queryParam("month", "2027-09")
            .when()
            .get("/reports/revenue")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .extract()
            .asString();

        Assertions.assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        Assertions.assertTrue(csv.contains(id1 + ",101,2027-09-10,2027-09-12,CONFIRMED,499.80"));
        Assertions.assertTrue(csv.contains("total,499.80"));
    }
}
