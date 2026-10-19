package br.com.mareaalta;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ReservationIT {

    @BeforeAll
    public static void setup() {
        RestAssured.port = 7070;
        RestAssured.baseURI = "http://localhost";

        new Thread(() -> App.main(new String[]{})).start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
    public void testCreateReservationWithCpf() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "101");
        request.put("guestName", "João Silva");
        request.put("guestEmail", "joao@example.com");
        request.put("guestCpf", "12345678900");
        request.put("checkIn", "2027-03-15");
        request.put("checkOut", "2027-03-17");
        request.put("guests", 2);
        request.put("ratePlan", "STANDARD");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("status", is("PENDING"))
            .body("guestName", is("João Silva"))
            .body("guestCpf", is("12345678900"))
            .body("nights", is(2))
            .body("subtotal", is(499.80f))
            .body("total", is(499.80f));
    }

    @Test
    public void testCreateReservationWithCnpj() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "201");
        request.put("guestName", "Ana Souza");
        request.put("guestEmail", "ana@example.com");
        request.put("companyCnpj", "12345678000190");
        request.put("companyName", "Tecnosul Ltda");
        request.put("checkIn", "2027-03-10");
        request.put("checkOut", "2027-03-12");
        request.put("guests", 2);
        request.put("ratePlan", "STANDARD");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("companyName", is("Tecnosul Ltda"))
            .body("companyCnpj", is("12345678000190"))
            .body("guestCpf", nullValue());
    }

    @Test
    public void testCreateReservationWithPassport() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "301");
        request.put("guestName", "John Smith");
        request.put("guestEmail", "john@example.com");
        request.put("guestPassport", "X1234567");
        request.put("guestCountry", "United Kingdom");
        request.put("checkIn", "2027-03-10");
        request.put("checkOut", "2027-03-12");
        request.put("guests", 2);
        request.put("ratePlan", "STANDARD");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("guestPassport", is("X1234567"))
            .body("guestCountry", is("United Kingdom"))
            .body("guestCpf", nullValue());
    }

    @Test
    public void testHighSeasonPricing() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "102");
        request.put("guestName", "Maria Costa");
        request.put("guestEmail", "maria@example.com");
        request.put("guestCpf", "98765432100");
        request.put("checkIn", "2027-07-10");
        request.put("checkOut", "2027-07-12");
        request.put("guests", 1);
        request.put("ratePlan", "STANDARD");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("nights", is(2))
            .body("subtotal", is(599.76f))
            .body("total", is(599.76f));
    }

    @Test
    public void testNonRefundableDiscount() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "202");
        request.put("guestName", "Pedro Alves");
        request.put("guestEmail", "pedro@example.com");
        request.put("guestCpf", "11122233344");
        request.put("checkIn", "2027-03-10");
        request.put("checkOut", "2027-03-12");
        request.put("guests", 2);
        request.put("ratePlan", "NON_REFUNDABLE");

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/reservations")
            .then()
            .statusCode(201)
            .body("subtotal", is(779.80f))
            .body("discount", is(116.97f))
            .body("total", is(662.83f));
    }

    @Test
    public void testConfirmReservation() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "101");
        request.put("guestName", "Carlos Dias");
        request.put("guestEmail", "carlos@example.com");
        request.put("guestCpf", "55566677788");
        request.put("checkIn", "2027-04-10");
        request.put("checkOut", "2027-04-12");
        request.put("guests", 1);
        request.put("ratePlan", "STANDARD");

        int reservationId = given()
            .contentType(ContentType.JSON)
            .body(request)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200)
            .body("status", is("CONFIRMED"))
            .body("amountPaid", is(499.80f));

        given()
            .get("/messages")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[-1].channel", is("EMAIL"))
            .body("[-1].to", is("carlos@example.com"))
            .body("[-1].subject", is("Reserva confirmada - Pousada Maré Alta"));
    }
}
