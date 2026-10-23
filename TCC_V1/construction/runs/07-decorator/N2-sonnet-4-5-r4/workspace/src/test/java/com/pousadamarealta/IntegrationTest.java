package com.pousadamarealta;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@Testcontainers
public class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    private static Thread appThread;

    @BeforeAll
    static void setUp() {
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USER", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("PORT", "7071");

        appThread = new Thread(() -> {
            App.main(new String[]{});
        });
        appThread.setDaemon(true);
        appThread.start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7071;
    }

    @AfterAll
    static void tearDown() {
        if (appThread != null) {
            appThread.interrupt();
        }
    }

    @BeforeEach
    void reset() {
        RestAssured.reset();
    }

    @Test
    void testGetRooms() {
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
    void testCreateReservation() {
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
            .body("roomNumber", is("101"))
            .body("guestName", is("João Silva"))
            .body("status", is("PENDING"))
            .body("nights", is(2))
            .body("subtotal", is(499.80f))
            .body("discount", is(0.0f))
            .body("total", is(499.80f))
            .body("amountPaid", is(0.0f));
    }

    @Test
    void testConfirmReservation() {
        String body = """
            {
                "roomNumber": "201",
                "guestName": "Maria Santos",
                "guestEmail": "maria@example.com",
                "guestCpf": "98765432109",
                "checkIn": "2027-03-15",
                "checkOut": "2027-03-17",
                "guests": 2,
                "ratePlan": "STANDARD"
            }
            """;

        int id = given()
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
            .post("/reservations/" + id + "/confirmation")
            .then()
            .statusCode(200)
            .body("status", is("CONFIRMED"))
            .body("amountPaid", is(779.80f));
    }

    @Test
    void testHighSeasonPricing() {
        String body = """
            {
                "roomNumber": "101",
                "guestName": "Test User",
                "guestEmail": "test@example.com",
                "guestCpf": "11111111111",
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
            .body("subtotal", is(599.76f))
            .body("total", is(599.76f));
    }

    @Test
    void testNonRefundableDiscount() {
        String body = """
            {
                "roomNumber": "301",
                "guestName": "Test User",
                "guestEmail": "test@example.com",
                "guestCpf": "22222222222",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
                "guests": 3,
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
            .body("subtotal", is(1159.80f))
            .body("discount", is(173.97f))
            .body("total", is(985.83f));
    }

    @Test
    void testRoomUnavailable() {
        String body1 = """
            {
                "roomNumber": "102",
                "guestName": "First Guest",
                "guestEmail": "first@example.com",
                "guestCpf": "33333333333",
                "checkIn": "2027-04-10",
                "checkOut": "2027-04-15",
                "guests": 1,
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
                "roomNumber": "102",
                "guestName": "Second Guest",
                "guestEmail": "second@example.com",
                "guestCpf": "44444444444",
                "checkIn": "2027-04-12",
                "checkOut": "2027-04-17",
                "guests": 1,
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
    void testCapacityExceeded() {
        String body = """
            {
                "roomNumber": "101",
                "guestName": "Test User",
                "guestEmail": "test@example.com",
                "guestCpf": "55555555555",
                "checkIn": "2027-05-10",
                "checkOut": "2027-05-12",
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
    void testEmailValidation() {
        String body = """
            {
                "roomNumber": "201",
                "guestName": "Invalid Email",
                "guestEmail": "invalid.email",
                "guestCpf": "66666666666",
                "checkIn": "2027-06-10",
                "checkOut": "2027-06-12",
                "guests": 1,
                "ratePlan": "STANDARD"
            }
            """;

        int id = given()
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
            .post("/reservations/" + id + "/confirmation")
            .then()
            .statusCode(200);

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("[-1].status", is("BLOCKED"))
            .body("[-1].to", is("invalid.email"));
    }
}
