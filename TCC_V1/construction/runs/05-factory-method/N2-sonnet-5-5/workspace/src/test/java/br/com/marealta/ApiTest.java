package br.com.marealta;

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
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        app = Main.build(new RoomCatalog(), IdentityTypes.standard(), new InMemoryReservationRepository(),
                new InMemoryMessageRepository()).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() { app.stop(); }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String reservation(int room, String ident, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Ana Souza\",\"guestEmail\":\"ana@x.com\"," + ident
                + ",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out + "\",\"guests\":" + guests
                + ",\"ratePlan\":\"" + plan + "\"}";
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void fullFlowWithCompany() throws Exception {
        String ident = "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"";
        HttpResponse<String> created = call("POST", "/reservations",
                reservation(201, ident, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        JsonNode r = mapper.readTree(created.body());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("guestCpf").isNull());
        assertEquals(779.80, r.get("total").asDouble());

        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode msgs = mapper.readTree(call("GET", "/messages", null).body());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertTrue(msgs.get(0).get("body").asText().contains("ISS retido (5%): R$ 38,99"));

        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        JsonNode doc = mapper.readTree(call("POST", "/reservations/1/check-out", null).body());
        assertEquals("Fatura de hospedagem", doc.get("title").asText());
        assertEquals("Tecnosul Ltda", doc.get("companyName").asText());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).statusCode());

        String occupancy = call("GET", "/reports/occupancy?date=2027-03-11", null).body();
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occupancy);
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, CPF + ",\"guestPassport\":\"A\",\"guestCountry\":\"UK\"", "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, "\"companyCnpj\":\"1\"", "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-10", "2027-03-12", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations",
                reservation(999, CPF, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-12", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).statusCode());
    }

    @Test
    void cancelledReservationFreesRoomAndRefunds() throws Exception {
        call("POST", "/reservations", reservation(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        JsonNode c = mapper.readTree(call("POST", "/reservations/1/cancellation", null).body());
        assertEquals(499.80, c.get("refund").asDouble());
        assertEquals(201, call("POST", "/reservations",
                reservation(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
    }
}
