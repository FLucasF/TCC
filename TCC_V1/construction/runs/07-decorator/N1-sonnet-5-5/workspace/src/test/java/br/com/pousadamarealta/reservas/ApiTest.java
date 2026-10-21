package br.com.pousadamarealta.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.pousadamarealta.reservas.web.App;
import br.com.pousadamarealta.reservas.web.Config;
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

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private Javalin app;

    @BeforeEach
    void start() {
        app = App.create(new Config(0, false)).start(0);
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String reservation(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void fullLifecycleWithReceiptEmailAndReports() throws Exception {
        HttpResponse<String> created = call("POST", "/reservations", reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        JsonNode r = mapper.readTree(created.body());
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(created.body().contains("\"amountPaid\":0.00"), created.body());
        assertTrue(r.get("refund").isNull());

        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        String confirmed = call("POST", "/reservations/1/confirmation", null).body();
        assertTrue(confirmed.contains("\"amountPaid\":779.80"), confirmed);
        assertEquals(409, call("POST", "/reservations/1/confirmation", null).statusCode());

        JsonNode messages = mapper.readTree(call("GET", "/messages", null).body());
        assertEquals(2, messages.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
        assertEquals("SENT", messages.get(0).get("status").asText());
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

        HttpResponse<String> occupancy = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                occupancy.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());
        String checkOut = call("POST", "/reservations/1/check-out", null).body();
        JsonNode receipt = mapper.readTree(checkOut);
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertTrue(checkOut.contains("\"total\":779.80"), checkOut);
        assertEquals("2027-03-10", receipt.get("checkIn").asText());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());

        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void nonRefundableCancellationKeepsMoney() throws Exception {
        call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        String cancelled = call("POST", "/reservations/1/cancellation", null).body();
        assertTrue(cancelled.contains("\"status\":\"CANCELLED\""), cancelled);
        assertTrue(cancelled.contains("\"refund\":0.00"), cancelled);
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,101,2027-03-10,2027-03-11,CANCELLED,212.42\ntotal,212.42\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "não é json").statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-10", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 0, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, "10/03/2027", "2027-03-11", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "{}").statusCode());
        assertEquals("ROOM_NOT_FOUND", mapper.readTree(call("POST", "/reservations",
                reservation(999, "2027-03-10", "2027-03-11", 1, "STANDARD")).body()).get("error").asText());
        assertEquals(422, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 3, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations", reservation(101, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", reservation(101, "2027-03-12", "2027-03-13", 2, "STANDARD")).statusCode());
        call("POST", "/reservations/1/cancellation", null);
        assertEquals(201, call("POST", "/reservations", reservation(101, "2027-03-11", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-3-1", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue", null).statusCode());
    }

    @Test
    void invalidGuestEmailIsBlockedOnConfirmation() throws Exception {
        call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 1, "STANDARD").replace("carla@example.com", "carla@gmail"));
        call("POST", "/reservations/1/confirmation", null);
        JsonNode messages = mapper.readTree(call("GET", "/messages", null).body());
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
    }

    @Test
    void roomsAreListed() throws Exception {
        String body = call("GET", "/rooms", null).body();
        assertTrue(body.contains("\"nightlyRate\":249.90"), body);
        JsonNode rooms = mapper.readTree(body);
        assertEquals(5, rooms.size());
        assertEquals(4, rooms.get(4).get("capacity").asInt());
    }
}
