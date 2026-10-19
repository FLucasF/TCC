package br.com.mareaalta;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ReportsIT {

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
    public void testOccupancyReport() {
        Map<String, Object> request1 = new HashMap<>();
        request1.put("roomNumber", "301");
        request1.put("guestName", "Guest Occupancy 1");
        request1.put("guestEmail", "guest-occ1@example.com");
        request1.put("guestCpf", "11111111111");
        request1.put("checkIn", "2028-01-10");
        request1.put("checkOut", "2028-01-12");
        request1.put("guests", 1);
        request1.put("ratePlan", "STANDARD");

        int id1 = given()
            .contentType(ContentType.JSON)
            .body(request1)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given().post("/reservations/" + id1 + "/confirmation");

        Map<String, Object> request2 = new HashMap<>();
        request2.put("roomNumber", "202");
        request2.put("guestName", "Guest Occupancy 2");
        request2.put("guestEmail", "guest-occ2@example.com");
        request2.put("guestCpf", "22222222222");
        request2.put("checkIn", "2028-01-09");
        request2.put("checkOut", "2028-01-11");
        request2.put("guests", 2);
        request2.put("ratePlan", "STANDARD");

        int id2 = given()
            .contentType(ContentType.JSON)
            .body(request2)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given().post("/reservations/" + id2 + "/confirmation");

        String csv = given()
            .get("/reports/occupancy?date=2028-01-10")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .extract()
            .asString();

        assert csv.contains("room,type,reservation,check_in,check_out");
        assert csv.contains("202,DELUXE," + id2 + ",2028-01-09,2028-01-11");
        assert csv.contains("301,SUITE," + id1 + ",2028-01-10,2028-01-12");
        assert csv.contains("total,2/5");
    }

    @Test
    public void testRevenueReport() {
        Map<String, Object> request1 = new HashMap<>();
        request1.put("roomNumber", "301");
        request1.put("guestName", "Revenue Guest 1");
        request1.put("guestEmail", "revenue1@example.com");
        request1.put("guestCpf", "33333333333");
        request1.put("checkIn", "2028-02-15");
        request1.put("checkOut", "2028-02-17");
        request1.put("guests", 2);
        request1.put("ratePlan", "STANDARD");

        int id1 = given()
            .contentType(ContentType.JSON)
            .body(request1)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given().post("/reservations/" + id1 + "/confirmation");

        Map<String, Object> request2 = new HashMap<>();
        request2.put("roomNumber", "301");
        request2.put("guestName", "Revenue Guest 2");
        request2.put("guestEmail", "revenue2@example.com");
        request2.put("guestCpf", "44444444444");
        request2.put("checkIn", "2028-02-20");
        request2.put("checkOut", "2028-02-22");
        request2.put("guests", 2);
        request2.put("ratePlan", "NON_REFUNDABLE");

        int id2 = given()
            .contentType(ContentType.JSON)
            .body(request2)
            .post("/reservations")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

        given().post("/reservations/" + id2 + "/confirmation");
        given().post("/reservations/" + id2 + "/cancellation");

        String csv = given()
            .get("/reports/revenue?month=2028-02")
            .then()
            .statusCode(200)
            .contentType("text/csv; charset=utf-8")
            .extract()
            .asString();

        assert csv.contains("reservation,room,check_in,check_out,status,amount");
        assert csv.contains(id1 + ",301,2028-02-15,2028-02-17,CONFIRMED,");
        assert csv.contains(id2 + ",301,2028-02-20,2028-02-22,CANCELLED,");
        assert csv.contains("total,");
    }
}
