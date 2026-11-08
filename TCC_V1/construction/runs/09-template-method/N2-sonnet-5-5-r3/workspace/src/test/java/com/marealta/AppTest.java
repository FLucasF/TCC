package com.marealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    private static final ObjectMapper JSON = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient client = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        app = new App(ds).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return JSON.readTree(r.body());
    }

    private void assertError(HttpResponse<String> r, int status, String code) {
        assertEquals(status, r.statusCode(), r.body());
        assertTrue(r.body().contains("\"" + code + "\""), r.body());
    }

    @Test
    void highSeasonPricing() throws Exception {
        JsonNode r = create(101, "2027-07-10", "2027-07-12", 2, "STANDARD");
        String raw = call("GET", "/reservations/1", null).body();
        assertTrue(raw.contains("\"amountPaid\":0.00") && raw.contains("\"total\":599.76") && raw.contains("\"refund\":null"), raw);
        assertEquals(1, r.get("id").asInt());
        assertEquals("599.76", r.get("total").decimalValue().toPlainString());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("refund").isNull());
        assertEquals(0, r.get("amountPaid").decimalValue().signum());
    }

    @Test
    void confirmationSendsEmailAndDocumentMatches() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode msgs = JSON.readTree(call("GET", "/messages", null).body());
        assertEquals(1, msgs.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", msgs.get(0).get("body").asText());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void lifecycleAndErrors() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE");
        assertError(call("GET", "/reservations/1/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations/1/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        call("POST", "/reservations/1/confirmation", null);
        assertError(call("POST", "/reservations/1/confirmation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode cancelled = JSON.readTree(call("POST", "/reservations/1/cancellation", null).body());
        assertEquals(0, cancelled.get("refund").decimalValue().signum());
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD");
    }

    @Test
    void validationAndConflicts() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(call("POST", "/reservations", booking(101, "2027-03-11", "2027-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        assertError(call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-13", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("POST", "/reservations", booking(999, "2027-03-12", "2027-03-13", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(101, "2027-03-13", "2027-03-13", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-03-12", "2027-03-13", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "nope"), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reservations/77", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/revenue?month=2027-13", null), 400, "INVALID_REQUEST");
    }

    @Test
    void reports() throws Exception {
        create(101, "2027-07-10", "2027-07-13", 2, "STANDARD");        // 1: paga
        create(201, "2027-07-11", "2027-07-12", 1, "NON_REFUNDABLE");  // 2: cancelada, não reembolsável
        create(202, "2027-07-12", "2027-07-14", 2, "STANDARD");        // 3: pendente
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/cancellation", null);

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2027-07-12", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-12
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-13
                total,1/5
                """, occ.body());

        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-13,CONFIRMED,899.64
                2,201,2027-07-11,2027-07-12,CANCELLED,397.70
                total,1297.34
                """, call("GET", "/reports/revenue?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                1,101,2027-07-10,2027-07-13,2,3,27.00
                total,27.00
                """, call("GET", "/reports/tourism-tax?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,1,899.64
                NON_REFUNDABLE,1,397.70
                total,2,1297.34
                """, call("GET", "/reports/rate-plans?month=2027-07", null).body());
    }
}
