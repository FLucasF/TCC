package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

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
    static final ObjectMapper M = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    Javalin app;
    HttpClient http = HttpClient.newHttpClient();
    String base;

    @BeforeEach
    void start() {
        app = new App(false).build().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    String res(int room, String in, String out, int guests, String plan, String email) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"" + email
                + "\",\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out
                + "\",\"guests\":" + guests + ",\"ratePlan\":\"" + plan + "\"}";
    }

    @Test
    void fullFlowAndEmail() throws Exception {
        var r = call("POST", "/reservations", res(201, "2027-03-10", "2027-03-12", 2, "STANDARD", "carla@x.com"));
        assertEquals(201, r.statusCode());
        JsonNode j = M.readTree(r.body());
        assertEquals(1, j.get("id").asInt());
        assertEquals("PENDING", j.get("status").asText());
        assertTrue(r.body().contains("\"amountPaid\":0.00"), r.body());
        assertTrue(j.get("refund").isNull());
        assertTrue(r.body().contains("\"total\":779.80"), r.body());

        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals("CONFIRMED", M.readTree(call("POST", "/reservations/1/confirmation", null).body()).get("status").asText());

        JsonNode msgs = M.readTree(call("GET", "/messages", null).body());
        assertEquals(2, msgs.size());
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
        assertEquals("SENT", msgs.get(1).get("status").asText());

        var occ = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(occ.headers().firstValue("content-type").orElse("").toLowerCase().contains("text/csv; charset=utf-8"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        var out = call("POST", "/reservations/1/check-out", null);
        JsonNode d = M.readTree(out.body());
        assertEquals("Comprovante de reserva", d.get("title").asText());
        assertEquals("DELUXE", d.get("roomType").asText());
        assertEquals(200, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());

        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void cancellationRefunds() throws Exception {
        call("POST", "/reservations", res(101, "2027-07-10", "2027-07-12", 2, "STANDARD", "a@b.com"));
        call("POST", "/reservations", res(102, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE", "a@b.com"));
        call("POST", "/reservations", res(301, "2027-07-10", "2027-07-12", 2, "STANDARD", "a@b.com"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        String b1 = call("POST", "/reservations/1/cancellation", null).body();
        assertTrue(b1.contains("\"refund\":599.76"), b1);
        String b2 = call("POST", "/reservations/2/cancellation", null).body();
        assertTrue(b2.contains("\"refund\":0.00"), b2);
        assertTrue(b2.contains("\"amountPaid\":509.80"), b2);
        String b3 = call("POST", "/reservations/3/cancellation", null).body();
        assertTrue(b3.contains("\"refund\":0.00") && b3.contains("\"amountPaid\":0.00"), b3);
        // cancelada libera o quarto
        assertEquals(201, call("POST", "/reservations", res(101, "2027-07-11", "2027-07-13", 1, "STANDARD", "a@b.com")).statusCode());
        // só a não reembolsável cancelada deixa dinheiro
        String rev = call("GET", "/reports/revenue?month=2027-07", null).body();
        assertTrue(rev.contains("2,102,2027-07-10,2027-07-12,CANCELLED,509.80"), rev);
        assertTrue(rev.endsWith("total,509.80\n"), rev);
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao json").statusCode());
        assertEquals(400, call("POST", "/reservations", "{}").statusCode());
        assertEquals(400, call("POST", "/reservations", res(101, "2027-03-10", "2027-03-10", 1, "STANDARD", "a@b.com")).statusCode());
        assertEquals(400, call("POST", "/reservations", res(101, "2027-03-10", "2027-03-12", 0, "STANDARD", "a@b.com")).statusCode());
        assertEquals(400, call("POST", "/reservations", res(101, "10/03/2027", "2027-03-12", 1, "STANDARD", "a@b.com")).statusCode());
        var nr = call("POST", "/reservations", res(999, "2027-03-10", "2027-03-12", 1, "STANDARD", "a@b.com"));
        assertEquals(404, nr.statusCode());
        assertEquals("ROOM_NOT_FOUND", M.readTree(nr.body()).get("error").asText());
        assertEquals(422, call("POST", "/reservations", res(101, "2027-03-10", "2027-03-12", 3, "STANDARD", "a@b.com")).statusCode());
        assertEquals(201, call("POST", "/reservations", res(101, "2027-03-10", "2027-03-12", 2, "STANDARD", "a@b.com")).statusCode());
        var u = call("POST", "/reservations", res(101, "2027-03-11", "2027-03-14", 2, "STANDARD", "a@b.com"));
        assertEquals(409, u.statusCode());
        assertEquals("ROOM_UNAVAILABLE", M.readTree(u.body()).get("error").asText());
        assertEquals(201, call("POST", "/reservations", res(101, "2027-03-12", "2027-03-14", 2, "STANDARD", "a@b.com")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(404, call("GET", "/reservations/abc", null).statusCode());
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-13-01", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
    }

    @Test
    void roomsAndBlockedEmail() throws Exception {
        JsonNode rooms = M.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertTrue(call("GET", "/rooms", null).body().contains("\"nightlyRate\":249.90"));
        call("POST", "/reservations", res(101, "2027-03-10", "2027-03-12", 2, "STANDARD", "carla@gmail"));
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode msgs = M.readTree(call("GET", "/messages", null).body());
        assertEquals(1, msgs.size());
        assertEquals("BLOCKED", msgs.get(0).get("status").asText());
        assertEquals("carla@gmail", msgs.get(0).get("to").asText());
    }
}
