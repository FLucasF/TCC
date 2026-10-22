package br.com.pousadamarealta;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationSystemIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("pousada_test")
            .withUsername("test")
            .withPassword("test");

    private static Thread appThread;

    @BeforeAll
    static void setUp() {
        String dbUrl = postgres.getJdbcUrl();
        String dbUser = postgres.getUsername();
        String dbPassword = postgres.getPassword();

        System.setProperty("DB_URL", dbUrl);
        System.setProperty("DB_USER", dbUser);
        System.setProperty("DB_PASSWORD", dbPassword);
        System.setProperty("PORT", "7071");

        appThread = new Thread(() -> {
            App.main(new String[]{});
        });
        appThread.setDaemon(true);
        appThread.start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7071;
    }

    @Test
    @Order(1)
    void shouldGetRooms() {
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
        Map<String, Object> reservation = Map.of(
            "roomNumber", "201",
            "guestName", "Carla Dias",
            "guestEmail", "carla@example.com",
            "guestCpf", "11122233344",
            "checkIn", "2027-03-10",
            "checkOut", "2027-03-12",
            "guests", 2,
            "ratePlan", "STANDARD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(reservation)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .body("id", is(1))
                .body("roomNumber", is("201"))
                .body("guestName", is("Carla Dias"))
                .body("status", is("PENDING"))
                .body("nights", is(2))
                .body("subtotal", is(779.80f))
                .body("discount", is(0.00f))
                .body("total", is(779.80f))
                .body("amountPaid", is(0.00f));
    }

    @Test
    @Order(3)
    void shouldCalculateHighSeasonPrice() {
        Map<String, Object> reservation = Map.of(
            "roomNumber", "101",
            "guestName", "João Silva",
            "guestEmail", "joao@example.com",
            "guestCpf", "12345678901",
            "checkIn", "2027-07-10",
            "checkOut", "2027-07-12",
            "guests", 2,
            "ratePlan", "STANDARD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(reservation)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .body("subtotal", is(599.76f));
    }

    @Test
    @Order(4)
    void shouldCalculateNonRefundableDiscount() {
        Map<String, Object> reservation = Map.of(
            "roomNumber", "102",
            "guestName", "Maria Santos",
            "guestEmail", "maria@example.com",
            "guestCpf", "98765432100",
            "checkIn", "2027-03-15",
            "checkOut", "2027-03-17",
            "guests", 2,
            "ratePlan", "NON_REFUNDABLE"
        );

        given()
            .contentType(ContentType.JSON)
            .body(reservation)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201)
                .body("subtotal", is(499.80f))
                .body("discount", is(74.97f))
                .body("total", is(424.83f));
    }

    @Test
    @Order(5)
    void shouldRejectReservationExceedingCapacity() {
        Map<String, Object> reservation = Map.of(
            "roomNumber", "101",
            "guestName", "Teste",
            "guestEmail", "teste@example.com",
            "guestCpf", "11111111111",
            "checkIn", "2027-04-01",
            "checkOut", "2027-04-02",
            "guests", 5,
            "ratePlan", "STANDARD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(reservation)
            .when()
                .post("/reservations")
            .then()
                .statusCode(422)
                .body("error", is("CAPACITY_EXCEEDED"));
    }

    @Test
    @Order(6)
    void shouldRejectConflictingReservation() {
        Map<String, Object> reservation = Map.of(
            "roomNumber", "201",
            "guestName", "Outro Hóspede",
            "guestEmail", "outro@example.com",
            "guestCpf", "22222222222",
            "checkIn", "2027-03-11",
            "checkOut", "2027-03-13",
            "guests", 2,
            "ratePlan", "STANDARD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(reservation)
            .when()
                .post("/reservations")
            .then()
                .statusCode(409)
                .body("error", is("ROOM_UNAVAILABLE"));
    }

    @Test
    @Order(7)
    void shouldConfirmReservationAndSendNotifications() {
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
                .body("size()", is(3))
                .body("find { it.to == 'carla@example.com' }.subject",
                    is("Reserva confirmada - Pousada Maré Alta"))
                .body("find { it.to == 'carla@example.com' }.body",
                    containsString("Comprovante de reserva nº 1"))
                .body("find { it.to == 'governanca@pousadamarealta.com.br' }.subject",
                    is("Reserva confirmada - quarto 201"))
                .body("find { it.to == 'financeiro@pousadamarealta.com.br' }.subject",
                    is("Pagamento recebido - reserva 1"));
    }

    @Test
    @Order(8)
    void shouldGetDocument() {
        given()
            .when()
                .get("/reservations/1/document")
            .then()
                .statusCode(200)
                .body("title", is("Comprovante de reserva"))
                .body("reservationId", is(1))
                .body("guestName", is("Carla Dias"))
                .body("roomNumber", is("201"));
    }

    @Test
    @Order(9)
    void shouldCheckIn() {
        given()
            .when()
                .post("/reservations/1/check-in")
            .then()
                .statusCode(200)
                .body("status", is("CHECKED_IN"));
    }

    @Test
    @Order(10)
    void shouldCheckOut() {
        given()
            .when()
                .post("/reservations/1/check-out")
            .then()
                .statusCode(200)
                .body("title", is("Comprovante de reserva"))
                .body("reservationId", is(1));
    }

    @Test
    @Order(11)
    void shouldCancelConfirmedReservationAndSendNotifications() {
        given()
            .when()
                .post("/reservations/2/confirmation")
            .then()
                .statusCode(200);

        int messageCountBefore = given()
            .when()
                .get("/messages")
            .then()
                .extract().path("size()");

        given()
            .when()
                .post("/reservations/2/cancellation")
            .then()
                .statusCode(200)
                .body("status", is("CANCELLED"))
                .body("refund", is(599.76f));

        given()
            .when()
                .get("/messages")
            .then()
                .statusCode(200)
                .body("size()", is(messageCountBefore + 2))
                .body("find { it.subject.contains('Reserva cancelada') }.to",
                    is("governanca@pousadamarealta.com.br"))
                .body("find { it.subject.contains('Reembolso') }.to",
                    is("financeiro@pousadamarealta.com.br"));
    }

    @Test
    @Order(12)
    void shouldCancelNonRefundableWithoutRefundNotification() {
        given()
            .when()
                .post("/reservations/3/confirmation")
            .then()
                .statusCode(200);

        int messageCountBefore = given()
            .when()
                .get("/messages")
            .then()
                .extract().path("size()");

        given()
            .when()
                .post("/reservations/3/cancellation")
            .then()
                .statusCode(200)
                .body("status", is("CANCELLED"))
                .body("refund", is(0.00f));

        given()
            .when()
                .get("/messages")
            .then()
                .statusCode(200)
                .body("size()", is(messageCountBefore + 1));
    }

    @Test
    @Order(13)
    void shouldGenerateOccupancyReport() {
        Map<String, Object> reservation = Map.of(
            "roomNumber", "301",
            "guestName", "Paulo Costa",
            "guestEmail", "paulo@example.com",
            "guestCpf", "33333333333",
            "checkIn", "2027-05-10",
            "checkOut", "2027-05-12",
            "guests", 3,
            "ratePlan", "STANDARD"
        );

        given()
            .contentType(ContentType.JSON)
            .body(reservation)
            .when()
                .post("/reservations")
            .then()
                .statusCode(201);

        given()
            .when()
                .post("/reservations/4/confirmation")
            .then()
                .statusCode(200);

        String csv = given()
            .queryParam("date", "2027-05-10")
            .when()
                .get("/reports/occupancy")
            .then()
                .statusCode(200)
                .contentType(containsString("text/csv"))
                .extract().asString();

        Assertions.assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        Assertions.assertTrue(csv.contains("301,SUITE,4,2027-05-10,2027-05-12"));
        Assertions.assertTrue(csv.contains("total,1/5"));
    }

    @Test
    @Order(14)
    void shouldGenerateRevenueReport() {
        String csv = given()
            .queryParam("month", "2027-03")
            .when()
                .get("/reports/revenue")
            .then()
                .statusCode(200)
                .contentType(containsString("text/csv"))
                .extract().asString();

        Assertions.assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        Assertions.assertTrue(csv.contains("3,102,2027-03-15,2027-03-17,CANCELLED,424.83"));
    }
}
