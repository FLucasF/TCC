package br.com.marealta.pousada;

import java.net.URI;
import java.net.http.*;

import io.javalin.Javalin;
import org.junit.jupiter.api.*;

import br.com.marealta.pousada.domain.*;
import br.com.marealta.pousada.web.Api;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        InMemory.Messages log = new InMemory.Messages();
        RoomCatalog rooms = new RoomCatalog();
        ReservationService service = new ReservationService(new InMemory.Reservations(), rooms, InMemory.listeners(log));
        app = new Api(service, rooms, log).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() { app.stop(); }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static final String BODY = """
            {"roomNumber":101,"guestName":"Ana","guestEmail":"a@x.com","guestCpf":"1","checkIn":"2027-07-10","checkOut":"2027-07-12","guests":2,"ratePlan":"STANDARD"}""";

    @Test
    void fullFlow() throws Exception {
        assertTrue(call("GET", "/rooms", null).body().contains("\"nightlyRate\":249.90"));
        var created = call("POST", "/reservations", BODY);
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":599.76"));
        assertTrue(created.body().contains("\"refund\":null"));
        assertTrue(created.body().contains("\"amountPaid\":0.00"));
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(3, call("GET", "/messages", null).body().split("\"channel\":\"EMAIL\"").length - 1);
        var occupancy = call("GET", "/reports/occupancy?date=2027-07-11", null);
        assertTrue(occupancy.headers().firstValue("content-type").get().toLowerCase().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD,1,2027-07-10,2027-07-12\ntotal,1/5\n", occupancy.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-07-12", null).body().endsWith("total,0/5\n"));
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,101,2027-07-10,2027-07-12,CONFIRMED,599.76\ntotal,599.76\n",
                call("GET", "/reports/revenue?month=2027-07", null).body());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao json").statusCode());
        assertEquals(400, call("POST", "/reservations", BODY.replace("\"guests\":2", "\"guests\":0")).statusCode());
        assertEquals(400, call("POST", "/reservations", BODY.replace("2027-07-10", "10/07/2027")).statusCode());
        assertEquals(404, call("POST", "/reservations", BODY.replace("101", "999")).statusCode());
        assertEquals(422, call("POST", "/reservations", BODY.replace("\"guests\":2", "\"guests\":5").replace("101", "301")).statusCode());
        var missing = call("GET", "/reservations/999", null);
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("RESERVATION_NOT_FOUND"));
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy", null).statusCode());
    }
}
