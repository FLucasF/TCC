package com.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = Main.build(new InMemoryReservationRepository(), new InMemoryMessageRepository()).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"11122233344",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        var r = send("POST", "/reservations", booking(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return json.readTree(r.body());
    }

    @Test
    void highSeasonStandardPrice() throws Exception {
        var raw = send("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD")).body();
        JsonNode r = json.readTree(raw);
        assertTrue(raw.contains("\"amountPaid\":0.00") && raw.contains("\"total\":599.76"), raw);
        assertEquals(1, r.get("id").asInt());
        assertEquals("599.76", r.get("total").decimalValue().toPlainString());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("refund").isNull());
    }

    @Test
    void confirmationSendsEmailWithReceipt() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        send("POST", "/reservations/1/confirmation", null);
        JsonNode messages = json.readTree(send("GET", "/messages", null).body());
        assertEquals(1, messages.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
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
                Até breve!""", messages.get(0).get("body").asText());
    }

    @Test
    void lifecycleAndErrors() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE");
        assertEquals(409, send("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, send("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, send("POST", "/reservations", booking(101, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, send("POST", "/reservations", booking(101, "2027-04-10", "2027-04-12", 3, "STANDARD")).statusCode());
        assertEquals(404, send("POST", "/reservations", booking(999, "2027-04-10", "2027-04-12", 1, "STANDARD")).statusCode());
        assertEquals(400, send("POST", "/reservations", booking(101, "2027-04-12", "2027-04-12", 1, "STANDARD")).statusCode());
        assertEquals(400, send("POST", "/reservations", "nao e json").statusCode());
        assertEquals(404, send("GET", "/reservations/42", null).statusCode());
        assertEquals("RESERVATION_NOT_FOUND", json.readTree(send("GET", "/reservations/42", null).body()).get("error").asText());

        send("POST", "/reservations/1/confirmation", null);
        String cancelledRaw = send("POST", "/reservations/1/cancellation", null).body();
        JsonNode cancelled = json.readTree(cancelledRaw);
        assertTrue(cancelledRaw.contains("\"refund\":0.00"), cancelledRaw);
        assertEquals("CANCELLED", cancelled.get("status").asText());
        // cancelada libera o quarto
        create(101, "2027-03-10", "2027-03-12", 1, "STANDARD");
    }

    @Test
    void checkOutReturnsReceipt() throws Exception {
        create(101, "2027-03-10", "2027-03-13", 2, "NON_REFUNDABLE");
        send("POST", "/reservations/1/confirmation", null);
        send("POST", "/reservations/1/check-in", null);
        JsonNode receipt = json.readTree(send("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("STANDARD", receipt.get("roomType").asText());
        assertEquals("112.46", receipt.get("discount").decimalValue().toPlainString());
        assertEquals(200, send("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void reports() throws Exception {
        create(101, "2027-07-10", "2027-07-13", 2, "STANDARD");
        create(102, "2027-07-11", "2027-07-12", 1, "NON_REFUNDABLE");
        create(201, "2027-07-20", "2027-07-21", 1, "STANDARD");
        send("POST", "/reservations/1/confirmation", null);
        send("POST", "/reservations/2/confirmation", null);
        send("POST", "/reservations/2/cancellation", null);

        var occ = send("GET", "/reports/occupancy?date=2027-07-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-13
                total,1/5
                """, occ.body());

        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-13,CONFIRMED,899.64
                2,102,2027-07-11,2027-07-12,CANCELLED,254.90
                total,1154.54
                """, send("GET", "/reports/revenue?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                1,101,2027-07-10,2027-07-13,2,3,27.00
                total,27.00
                """, send("GET", "/reports/tourism-tax?month=2027-07", null).body());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,1,899.64
                NON_REFUNDABLE,1,254.90
                total,2,1154.54
                """, send("GET", "/reports/rate-plans?month=2027-07", null).body());

        assertEquals(400, send("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, send("GET", "/reports/occupancy", null).statusCode());
    }

    @Test
    void roomsList() throws Exception {
        String raw = send("GET", "/rooms", null).body();
        JsonNode rooms = json.readTree(raw);
        assertTrue(raw.contains("\"nightlyRate\":579.90"), raw);
        assertEquals(5, rooms.size());
    }
}
