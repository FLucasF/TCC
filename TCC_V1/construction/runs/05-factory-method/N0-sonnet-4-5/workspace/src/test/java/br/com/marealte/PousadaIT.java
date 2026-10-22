package br.com.marealte;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PousadaIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    private static Thread serverThread;

    @BeforeAll
    static void setUp() {
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USER", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("PORT", "7070");

        serverThread = new Thread(() -> Main.main(new String[]{}));
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 7070;
    }

    @Test
    @Order(1)
    void shouldGetRooms() {
        given()
            .when()
            .get("/rooms")
            .then()
            .statusCode(200)
            .body("size()", equalTo(5))
            .body("[0].number", equalTo("101"))
            .body("[0].type", equalTo("STANDARD"))
            .body("[0].capacity", equalTo(2))
            .body("[0].nightlyRate", equalTo(249.90f));
    }

    @Test
    @Order(2)
    void shouldCreateReservationWithCPF() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "João Silva",
                "guestEmail": "joao@email.com",
                "guestCpf": "123.456.789-00",
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
            .body("id", equalTo(1))
            .body("status", equalTo("PENDING"))
            .body("guestCpf", equalTo("123.456.789-00"))
            .body("companyCnpj", nullValue())
            .body("guestPassport", nullValue())
            .body("nights", equalTo(2))
            .body("subtotal", equalTo(779.80f))
            .body("discount", equalTo(0.00f))
            .body("total", equalTo(779.80f))
            .body("amountPaid", equalTo(0.00f));
    }

    @Test
    @Order(3)
    void shouldCreateReservationWithCNPJ() {
        String requestBody = """
            {
                "roomNumber": "201",
                "guestName": "Ana Costa",
                "guestEmail": "ana@email.com",
                "companyCnpj": "12.345.678/0001-90",
                "companyName": "Empresa ABC Ltda",
                "checkIn": "2027-03-15",
                "checkOut": "2027-03-17",
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
            .body("id", equalTo(2))
            .body("guestCpf", nullValue())
            .body("companyCnpj", equalTo("12.345.678/0001-90"))
            .body("companyName", equalTo("Empresa ABC Ltda"));
    }

    @Test
    @Order(4)
    void shouldCreateReservationWithPassport() {
        String requestBody = """
            {
                "roomNumber": "301",
                "guestName": "John Smith",
                "guestEmail": "john@email.com",
                "guestPassport": "X1234567",
                "guestCountry": "United Kingdom",
                "checkIn": "2027-03-20",
                "checkOut": "2027-03-22",
                "guests": 3,
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
            .body("id", equalTo(3))
            .body("guestPassport", equalTo("X1234567"))
            .body("guestCountry", equalTo("United Kingdom"))
            .body("subtotal", equalTo(1159.80f))
            .body("discount", equalTo(173.97f))
            .body("total", equalTo(985.83f));
    }

    @Test
    @Order(5)
    void shouldRejectReservationWithoutIdentification() {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Maria Santos",
                "guestEmail": "maria@email.com",
                "checkIn": "2027-04-01",
                "checkOut": "2027-04-03",
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
            .statusCode(400)
            .body("error", equalTo("INVALID_REQUEST"));
    }

    @Test
    @Order(6)
    void shouldRejectReservationWithMultipleIdentifications() {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Pedro Oliveira",
                "guestEmail": "pedro@email.com",
                "guestCpf": "111.222.333-44",
                "companyCnpj": "12.345.678/0001-90",
                "companyName": "Empresa XYZ",
                "checkIn": "2027-04-01",
                "checkOut": "2027-04-03",
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
            .statusCode(400)
            .body("error", equalTo("INVALID_REQUEST"));
    }

    @Test
    @Order(7)
    void shouldCalculateHighSeasonPrices() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Carlos Pereira",
                "guestEmail": "carlos@email.com",
                "guestCpf": "999.888.777-66",
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
            .body("nights", equalTo(2))
            .body("subtotal", equalTo(599.76f))
            .body("total", equalTo(599.76f));
    }

    @Test
    @Order(8)
    void shouldRejectRoomNotFound() {
        String requestBody = """
            {
                "roomNumber": "999",
                "guestName": "Test",
                "guestEmail": "test@email.com",
                "guestCpf": "111.111.111-11",
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
            .statusCode(404)
            .body("error", equalTo("ROOM_NOT_FOUND"));
    }

    @Test
    @Order(9)
    void shouldRejectCapacityExceeded() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Test",
                "guestEmail": "test@email.com",
                "guestCpf": "111.111.111-11",
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
            .body("error", equalTo("CAPACITY_EXCEEDED"));
    }

    @Test
    @Order(10)
    void shouldRejectRoomUnavailable() {
        String requestBody = """
            {
                "roomNumber": "101",
                "guestName": "Test Conflict",
                "guestEmail": "conflict@email.com",
                "guestCpf": "222.222.222-22",
                "checkIn": "2027-03-10",
                "checkOut": "2027-03-12",
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
            .statusCode(409)
            .body("error", equalTo("ROOM_UNAVAILABLE"));
    }

    @Test
    @Order(11)
    void shouldConfirmReservation() {
        given()
            .when()
            .post("/reservations/1/confirmation")
            .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"))
            .body("amountPaid", equalTo(779.80f));

        given()
            .when()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].channel", equalTo("EMAIL"))
            .body("[0].to", equalTo("joao@email.com"))
            .body("[0].subject", equalTo("Reserva confirmada - Pousada Maré Alta"))
            .body("[0].body", containsString("João Silva"))
            .body("[0].body", containsString("Comprovante de reserva nº 1"))
            .body("[0].body", containsString("CPF 123.456.789-00"));
    }

    @Test
    @Order(12)
    void shouldCheckIn() {
        given()
            .when()
            .post("/reservations/1/check-in")
            .then()
            .statusCode(200)
            .body("status", equalTo("CHECKED_IN"));
    }

    @Test
    @Order(13)
    void shouldCheckOutWithComprovante() {
        given()
            .when()
            .post("/reservations/1/check-out")
            .then()
            .statusCode(200)
            .body("title", equalTo("Comprovante de reserva"))
            .body("reservationId", equalTo(1))
            .body("guestName", equalTo("João Silva"))
            .body("guestCpf", equalTo("123.456.789-00"))
            .body("companyCnpj", nullValue())
            .body("guestPassport", nullValue())
            .body("roomNumber", equalTo("101"))
            .body("roomType", equalTo("STANDARD"))
            .body("nights", equalTo(2))
            .body("subtotal", equalTo(779.80f))
            .body("discount", equalTo(0.00f))
            .body("total", equalTo(779.80f))
            .body("text", containsString("Comprovante de reserva nº 1"))
            .body("text", containsString("João Silva (CPF 123.456.789-00)"))
            .body("text", containsString("Quarto: 101 (STANDARD)"))
            .body("text", containsString("Entrada: 10/03/2027"))
            .body("text", containsString("Saída: 12/03/2027"))
            .body("text", containsString("2 diárias"))
            .body("text", containsString("Diárias: R$ 779,80"))
            .body("text", containsString("Total: R$ 779,80"));
    }

    @Test
    @Order(14)
    void shouldGetDocumentSecondCopy() {
        given()
            .when()
            .get("/reservations/1/document")
            .then()
            .statusCode(200)
            .body("title", equalTo("Comprovante de reserva"))
            .body("text", containsString("Comprovante de reserva nº 1"));
    }

    @Test
    @Order(15)
    void shouldConfirmAndCheckOutCNPJReservation() {
        given().post("/reservations/2/confirmation").then().statusCode(200);
        given().post("/reservations/2/check-in").then().statusCode(200);

        given()
            .when()
            .post("/reservations/2/check-out")
            .then()
            .statusCode(200)
            .body("title", equalTo("Fatura de hospedagem"))
            .body("companyCnpj", equalTo("12.345.678/0001-90"))
            .body("companyName", equalTo("Empresa ABC Ltda"))
            .body("text", containsString("Fatura de hospedagem nº 2"))
            .body("text", containsString("Empresa: Empresa ABC Ltda (CNPJ 12.345.678/0001-90)"))
            .body("text", containsString("Hóspede: Ana Costa"))
            .body("text", containsString("ISS retido (5%):"));
    }

    @Test
    @Order(16)
    void shouldConfirmAndCheckOutPassportReservation() {
        given().post("/reservations/3/confirmation").then().statusCode(200);
        given().post("/reservations/3/check-in").then().statusCode(200);

        given()
            .when()
            .post("/reservations/3/check-out")
            .then()
            .statusCode(200)
            .body("title", equalTo("Booking receipt"))
            .body("guestPassport", equalTo("X1234567"))
            .body("guestCountry", equalTo("United Kingdom"))
            .body("text", containsString("Booking receipt no. 3"))
            .body("text", containsString("Guest: John Smith (passport X1234567, United Kingdom)"))
            .body("text", containsString("Check-in: 2027-03-20"))
            .body("text", containsString("Check-out: 2027-03-22"))
            .body("text", containsString("2 nights"))
            .body("text", containsString("Nights: BRL 1,159.80"))
            .body("text", containsString("Discount: BRL 173.97"))
            .body("text", containsString("Total: BRL 985.83"));
    }

    @Test
    @Order(17)
    void shouldCancelStandardReservationWithRefund() {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Test Cancel",
                "guestEmail": "cancel@email.com",
                "guestCpf": "333.333.333-33",
                "checkIn": "2027-06-01",
                "checkOut": "2027-06-02",
                "guests": 1,
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

        given().post("/reservations/" + reservationId + "/confirmation").then().statusCode(200);

        given()
            .when()
            .post("/reservations/" + reservationId + "/cancellation")
            .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"))
            .body("refund", equalTo(249.90f));
    }

    @Test
    @Order(18)
    void shouldCancelNonRefundableReservationWithoutRefund() {
        String requestBody = """
            {
                "roomNumber": "102",
                "guestName": "Test No Refund",
                "guestEmail": "norefund@email.com",
                "guestCpf": "444.444.444-44",
                "checkIn": "2027-06-05",
                "checkOut": "2027-06-06",
                "guests": 1,
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

        given().post("/reservations/" + reservationId + "/confirmation").then().statusCode(200);

        given()
            .when()
            .post("/reservations/" + reservationId + "/cancellation")
            .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"))
            .body("refund", equalTo(0.00f));
    }

    @Test
    @Order(19)
    void shouldGenerateOccupancyReport() {
        String csv = given()
            .queryParam("date", "2027-03-10")
            .when()
            .get("/reports/occupancy")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .extract().asString();

        Assertions.assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        Assertions.assertTrue(csv.contains("101,STANDARD,1,2027-03-10,2027-03-12"));
        Assertions.assertTrue(csv.contains("total,1/5"));
    }

    @Test
    @Order(20)
    void shouldGenerateRevenueReport() {
        String csv = given()
            .queryParam("month", "2027-03")
            .when()
            .get("/reports/revenue")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .extract().asString();

        Assertions.assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        Assertions.assertTrue(csv.contains("1,101,2027-03-10,2027-03-12,CHECKED_OUT,779.8"));
        Assertions.assertTrue(csv.contains("2,201,2027-03-15,2027-03-17,CHECKED_OUT,779.8"));
        Assertions.assertTrue(csv.contains("3,301,2027-03-20,2027-03-22,CHECKED_OUT,985.83"));
    }
}
