package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTest {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private Javalin app;

    @AfterEach
    void stop() {
        if (app != null) app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String email, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    @Test
    void fullFlowLiveMode() throws Exception {
        app = HotelApp.create(false).start(0);
        HttpResponse<String> created = call("POST", "/reservations",
                booking(201, "carla@x.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertTrue(created.body().contains("\"refund\":null"));

        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(409, call("POST", "/reservations",
                booking(201, "c@x.com", "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());

        JsonNode messages = json.readTree(call("GET", "/messages", null).body());
        assertEquals(2, messages.size());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).get("subject").asText());
        assertTrue(messages.get(0).get("body").asText().contains(
                "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n"
                        + "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada"));

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertTrue(occ.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/check-out", null).statusCode());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void errorsAndCancellation() throws Exception {
        app = HotelApp.create(true).start(0);
        assertEquals(400, call("POST", "/reservations", "nao json").statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-10", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-11", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", booking(999, "a@b.com", "2027-03-10", "2027-03-11", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-11", 3, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/9", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).statusCode());

        call("POST", "/reservations", booking(101, "ana@gmail", "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE"));
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        call("POST", "/reservations/1/confirmation", null);
        JsonNode messages = json.readTree(call("GET", "/messages", null).body());
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
        assertEquals("ana@gmail", messages.get(0).get("to").asText());

        String cancelledBody = call("POST", "/reservations/1/cancellation", null).body();
        JsonNode cancelled = json.readTree(cancelledBody);
        assertTrue(cancelledBody.contains("\"refund\":0.00"), cancelledBody);
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
        assertTrue(call("GET", "/reports/revenue?month=2027-03", null).body().contains(",CANCELLED,212.42\n"));
    }
}
