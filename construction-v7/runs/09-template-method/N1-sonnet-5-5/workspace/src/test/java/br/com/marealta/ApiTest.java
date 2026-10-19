package br.com.marealta;

import br.com.marealta.http.Api;
import br.com.marealta.reports.*;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiTest {
    Javalin app;
    String base;
    final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void start() {
        Outbox outbox = new Outbox();
        ReservationService service = new ReservationService(outbox);
        app = new Api(service, outbox, List.of(
                new OccupancyReport(service), new RevenueReport(service),
                new TourismTaxReport(service, Money.of("4.50")), new RatePlanReport(service))).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    String reservation(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void highSeasonPricing() throws Exception {
        var r = call("POST", "/reservations", reservation(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"subtotal\":599.76"), r.body());
        assertTrue(r.body().contains("\"refund\":null"), r.body());
    }

    @Test
    void confirmationSendsEmailWithReceipt() throws Exception {
        call("POST", "/reservations", reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        var messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"), messages);
        assertTrue(messages.contains("Diárias: R$ 779,80\\nTotal: R$ 779,80\\n\\nA entrada"), messages);
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void errors() throws Exception {
        assertEquals("INVALID_REQUEST", err(call("POST", "/reservations", "{oops")));
        assertEquals("ROOM_NOT_FOUND", err(call("POST", "/reservations", reservation(999, "2027-03-10", "2027-03-12", 1, "STANDARD"))));
        assertEquals("CAPACITY_EXCEEDED", err(call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-12", 3, "STANDARD"))));
        assertEquals("INVALID_REQUEST", err(call("POST", "/reservations", reservation(101, "2027-03-12", "2027-03-12", 1, "STANDARD"))));
        call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-12", 1, "STANDARD"));
        assertEquals("ROOM_UNAVAILABLE", err(call("POST", "/reservations", reservation(101, "2027-03-11", "2027-03-13", 1, "STANDARD"))));
        assertEquals("RESERVATION_NOT_FOUND", err(call("GET", "/reservations/42", null)));
        assertEquals("OPERATION_NOT_ALLOWED", err(call("POST", "/reservations/1/check-in", null)));
        assertEquals("OPERATION_NOT_ALLOWED", err(call("GET", "/reservations/1/document", null)));
    }

    String err(HttpResponse<String> r) {
        var m = java.util.regex.Pattern.compile("\"error\":\"(\\w+)\"").matcher(r.body());
        assertTrue(m.find(), r.body());
        return m.group(1);
    }

    @Test
    void reports() throws Exception {
        // 1: não reembolsável cancelada após confirmar; 2: standard confirmada; 3: pendente
        call("POST", "/reservations", reservation(101, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations", reservation(202, "2027-07-10", "2027-07-13", 2, "STANDARD"));
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations", reservation(301, "2027-07-11", "2027-07-12", 1, "STANDARD"));
        call("POST", "/reservations/1/cancellation", null);

        var revenue = call("GET", "/reports/revenue?month=2027-07", null);
        assertEquals("text/csv; charset=utf-8", revenue.headers().firstValue("content-type").orElse(""));
        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CANCELLED,509.80
                2,202,2027-07-10,2027-07-13,CONFIRMED,1403.64
                total,1913.44
                """, revenue.body());

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                2,202,2027-07-10,2027-07-13,2,3,27.00
                total,27.00
                """, call("GET", "/reports/tourism-tax?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,1,1403.64
                NON_REFUNDABLE,1,509.80
                total,2,1913.44
                """, call("GET", "/reports/rate-plans?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                202,DELUXE,2,2027-07-10,2027-07-13
                total,1/5
                """, call("GET", "/reports/occupancy?date=2027-07-11", null).body());

        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy", null).statusCode());
    }
}
