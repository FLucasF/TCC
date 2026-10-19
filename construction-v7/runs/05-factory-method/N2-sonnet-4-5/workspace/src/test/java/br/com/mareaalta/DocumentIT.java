package br.com.mareaalta;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class DocumentIT {

    @BeforeAll
    public static void setup() {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = 7070;
            RestAssured.baseURI = "http://localhost";

            new Thread(() -> App.main(new String[]{})).start();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testCpfDocument() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "101");
        request.put("guestName", "Carla Dias");
        request.put("guestEmail", "carla@example.com");
        request.put("guestCpf", "11122233344");
        request.put("checkIn", "2027-03-10");
        request.put("checkOut", "2027-03-12");
        request.put("guests", 2);
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
            .statusCode(200);

        given()
            .post("/reservations/" + reservationId + "/check-in")
            .then()
            .statusCode(200);

        String text = given()
            .post("/reservations/" + reservationId + "/check-out")
            .then()
            .statusCode(200)
            .body("title", is("Comprovante de reserva"))
            .extract()
            .path("text");

        assert text.contains("Comprovante de reserva nº");
        assert text.contains("Hóspede: Carla Dias (CPF 111.222.333-44)");
        assert text.contains("Quarto: 101 (STANDARD)");
        assert text.contains("Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias");
        assert text.contains("499,80");
    }

    @Test
    public void testCnpjDocument() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "102");
        request.put("guestName", "Ana Souza");
        request.put("guestEmail", "ana@example.com");
        request.put("companyCnpj", "12345678000190");
        request.put("companyName", "Tecnosul Ltda");
        request.put("checkIn", "2027-04-10");
        request.put("checkOut", "2027-04-12");
        request.put("guests", 2);
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
            .statusCode(200);

        given()
            .post("/reservations/" + reservationId + "/check-in")
            .then()
            .statusCode(200);

        String text = given()
            .post("/reservations/" + reservationId + "/check-out")
            .then()
            .statusCode(200)
            .body("title", is("Fatura de hospedagem"))
            .extract()
            .path("text");

        assert text.contains("Fatura de hospedagem nº");
        assert text.contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)");
        assert text.contains("Hóspede: Ana Souza");
        assert text.contains("ISS retido (5%):");
        assert text.contains("24,99");
    }

    @Test
    public void testPassportDocument() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "201");
        request.put("guestName", "John Smith");
        request.put("guestEmail", "john@example.com");
        request.put("guestPassport", "X1234567");
        request.put("guestCountry", "United Kingdom");
        request.put("checkIn", "2027-05-10");
        request.put("checkOut", "2027-05-12");
        request.put("guests", 2);
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
            .statusCode(200);

        given()
            .post("/reservations/" + reservationId + "/check-in")
            .then()
            .statusCode(200);

        String text = given()
            .post("/reservations/" + reservationId + "/check-out")
            .then()
            .statusCode(200)
            .body("title", is("Booking receipt"))
            .extract()
            .path("text");

        assert text.contains("Booking receipt no.");
        assert text.contains("Guest: John Smith (passport X1234567, United Kingdom)");
        assert text.contains("Room: 201 (DELUXE)");
        assert text.contains("Check-in: 2027-05-10 | Check-out: 2027-05-12 | 2 nights");
        assert text.contains("Nights: BRL 779.80");
        assert text.contains("Total: BRL 779.80");
    }

    @Test
    public void testGetDocument() {
        Map<String, Object> request = new HashMap<>();
        request.put("roomNumber", "202");
        request.put("guestName", "Test User");
        request.put("guestEmail", "test@example.com");
        request.put("guestCpf", "99988877766");
        request.put("checkIn", "2027-09-10");
        request.put("checkOut", "2027-09-11");
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
            .get("/reservations/" + reservationId + "/document")
            .then()
            .statusCode(409)
            .body("error", is("OPERATION_NOT_ALLOWED"));

        given()
            .post("/reservations/" + reservationId + "/confirmation")
            .then()
            .statusCode(200);

        given()
            .get("/reservations/" + reservationId + "/document")
            .then()
            .statusCode(200)
            .body("title", is("Comprovante de reserva"))
            .body("text", containsString("Test User"));
    }
}
