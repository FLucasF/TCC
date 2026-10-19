package br.com.mare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTest {
    static final ObjectMapper M = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    Javalin app;
    HttpClient http = HttpClient.newHttpClient();
    String base;

    @BeforeEach void start() { app = Main.createApp(new Hotel()).start(0); base = "http://localhost:" + app.port(); }
    @AfterEach void stop() { app.stop(); }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode book(int room, String in, String out, int guests, String plan, String extra) throws Exception {
        var r = call("POST", "/reservations", """
            {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
             "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"%s}""".formatted(room, in, out, guests, plan, extra));
        return M.readTree(r.body());
    }

    @Test void highSeasonStandard() throws Exception {
        JsonNode r = book(101, "2027-07-10", "2027-07-12", 2, "STANDARD", "");
        assertEquals("599.76", r.get("total").asText());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("refund").isNull());
    }

    @Test void corporate() throws Exception {
        JsonNode r = book(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", ",\"companyCode\":\"TECNOSUL\"");
        assertEquals("779.80", r.get("subtotal").asText());
        assertEquals("93.58", r.get("discount").asText());
        assertEquals("686.22", r.get("total").asText());
        assertEquals(400, call("POST", "/reservations", """
            {"roomNumber":201,"guestName":"a","guestEmail":"b","guestCpf":"c","checkIn":"2027-03-10","checkOut":"2027-03-12",
             "guests":2,"ratePlan":"CORPORATE","companyCode":"XYZ"}""").statusCode());
    }

    @Test void weekendPackageAndRefund() throws Exception {
        JsonNode r = book(201, "2027-03-12", "2027-03-14", 2, "WEEKEND_PACKAGE", "");
        assertEquals("820.00", r.get("total").asText());
        call("POST", "/reservations/1/confirmation", null);
        JsonNode c = M.readTree(call("POST", "/reservations/1/cancellation", null).body());
        assertEquals("410.00", c.get("refund").asText());
        assertEquals("CANCELLED", c.get("status").asText());
        assertEquals(400, call("POST", "/reservations", """
            {"roomNumber":201,"guestName":"a","guestEmail":"b","guestCpf":"c","checkIn":"2027-03-13","checkOut":"2027-03-15",
             "guests":2,"ratePlan":"WEEKEND_PACKAGE"}""").statusCode());
    }

    @Test void longStay() throws Exception {
        JsonNode r = book(101, "2027-03-01", "2027-03-08", 1, "LONG_STAY", "");
        assertEquals("1749.30", r.get("subtotal").asText());
        assertEquals("174.93", r.get("discount").asText());
        assertEquals("1574.37", r.get("total").asText());
        assertEquals(400, call("POST", "/reservations", """
            {"roomNumber":102,"guestName":"a","guestEmail":"b","guestCpf":"c","checkIn":"2027-03-01","checkOut":"2027-03-05",
             "guests":1,"ratePlan":"LONG_STAY"}""").statusCode());
    }

    @Test void lifecycleEmailAndReports() throws Exception {
        book(201, "2027-03-10", "2027-03-12", 2, "STANDARD", "");
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        JsonNode msgs = M.readTree(call("GET", "/messages", null).body());
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
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());
        assertEquals("total,1/5", call("GET", "/reports/occupancy?date=2027-03-11", null).body().lines().reduce((a, b) -> b).get());
        assertEquals("total,0/5", call("GET", "/reports/occupancy?date=2027-03-12", null).body().lines().reduce((a, b) -> b).get());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        call("POST", "/reservations/1/check-in", null);
        JsonNode doc = M.readTree(call("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        String rev = call("GET", "/reports/revenue?month=2027-03", null).body();
        assertTrue(rev.contains("1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80"), rev);
        assertTrue(rev.endsWith("total,779.80\n"), rev);
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).statusCode());
    }

    @Test void errors() throws Exception {
        assertEquals(404, call("GET", "/reservations/9", null).statusCode());
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(404, book2(999, 2).statusCode());
        assertEquals(422, book2(101, 3).statusCode());
        assertEquals(201, book2(101, 2).statusCode());
        assertEquals(409, book2(101, 2).statusCode());
        assertEquals(200, call("GET", "/rooms", null).statusCode());
    }

    HttpResponse<String> book2(int room, int guests) throws Exception {
        return call("POST", "/reservations", """
            {"roomNumber":%d,"guestName":"a","guestEmail":"b","guestCpf":"c","checkIn":"2027-03-10","checkOut":"2027-03-12",
             "guests":%d,"ratePlan":"STANDARD"}""".formatted(room, guests));
    }
}
