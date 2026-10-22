package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;
import pousada.infra.Database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private final HttpClient http = HttpClient.newHttpClient();
    private String url;
    private String schema;
    private Database db;
    private Javalin app;
    private String base;

    @BeforeAll
    void start() throws Exception {
        String dbUrl = System.getenv("DB_URL");
        Assumptions.assumeTrue(dbUrl != null, "DB_URL não definida");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(dbUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        url = dbUrl + (dbUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        db = new Database(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        app = Main.build(db).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
                 Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody());
        else b.method(method, HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return JSON.readTree(r.body());
    }

    private static String body(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private long create(int room, String in, String out, int guests, String plan) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", body(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        return json(r).get("id").asLong();
    }

    private void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, json(r).get("error").asText());
    }

    @Test
    void listsRooms() throws Exception {
        JsonNode rooms = json(call("GET", "/rooms", null));
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertEquals("STANDARD", rooms.get(0).get("type").asText());
        assertEquals(2, rooms.get(0).get("capacity").asInt());
        assertEquals("249.90", rooms.get(0).get("nightlyRate").decimalValue().toPlainString());
    }

    @Test
    void createsReservation() throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", body(102, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        JsonNode j = json(r);
        assertEquals("PENDING", j.get("status").asText());
        assertTrue(r.body().contains("\"total\":599.76") && r.body().contains("\"refund\":null"), r.body());
        assertEquals("2027-07-10", j.get("checkIn").asText());
        assertEquals(2, j.get("nights").asInt());
        assertEquals("599.76", j.get("total").decimalValue().toPlainString());
        assertEquals("0.00", j.get("amountPaid").decimalValue().toPlainString());
        assertTrue(j.get("refund").isNull());
        assertEquals(j.get("id").asLong(), json(call("GET", "/reservations/" + j.get("id").asLong(), null)).get("id").asLong());
    }

    @Test
    void validationErrors() throws Exception {
        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-07-10", "2027-07-10", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "10/07/2027", "2027-07-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 1, "OUTRO")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", body(999, "2027-07-10", "2027-07-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", body(101, "2027-07-10", "2027-07-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("GET", "/reservations/999999", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("POST", "/reservations/999999/confirmation", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/occupancy?date=ontem", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue?month=2027-13", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
    }

    @Test
    void roomCannotBeDoubleBookedUnlessCancelled() throws Exception {
        long first = create(202, "2028-04-10", "2028-04-12", 2, "STANDARD");
        assertError(call("POST", "/reservations", body(202, "2028-04-11", "2028-04-13", 2, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        create(202, "2028-04-12", "2028-04-14", 2, "STANDARD"); // saída e entrada no mesmo dia
        assertEquals(200, call("POST", "/reservations/" + first + "/cancellation", null).statusCode());
        create(202, "2028-04-11", "2028-04-12", 2, "STANDARD");
    }

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() throws Exception {
        int before = json(call("GET", "/messages", null)).size();
        long id = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(before, json(call("GET", "/messages", null)).size());

        JsonNode confirmed = json(call("POST", "/reservations/" + id + "/confirmation", null));
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("779.80", confirmed.get("amountPaid").decimalValue().toPlainString());
        assertError(call("POST", "/reservations/" + id + "/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode messages = json(call("GET", "/messages", null));
        assertEquals(before + 3, messages.size());
        JsonNode guest = find(messages, "carla@example.com");
        assertEquals("EMAIL", guest.get("channel").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", guest.get("subject").asText());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº %d
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""".formatted(id), guest.get("body").asText());
        JsonNode house = find(messages, "governanca@pousadamarealta.com.br");
        assertEquals("Reserva confirmada - quarto 201", house.get("subject").asText());
        assertTrue(house.get("body").asText().contains("10/03/2027"));
        assertTrue(house.get("body").asText().contains("12/03/2027"));
        JsonNode fin = find(messages, "financeiro@pousadamarealta.com.br");
        assertEquals("Pagamento recebido - reserva " + id, fin.get("subject").asText());
        assertTrue(fin.get("body").asText().contains("R$ 779,80"));
    }

    private JsonNode find(JsonNode messages, String to) {
        JsonNode found = null;
        for (JsonNode m : messages) if (m.get("to").asText().equals(to)) found = m; // a última
        assertNotNull(found, to);
        return found;
    }

    @Test
    void cancellationNotificationsFollowRefund() throws Exception {
        long normal = create(301, "2027-09-01", "2027-09-03", 2, "STANDARD");
        call("POST", "/reservations/" + normal + "/confirmation", null);
        int before = json(call("GET", "/messages", null)).size();
        JsonNode cancelled = json(call("POST", "/reservations/" + normal + "/cancellation", null));
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals("1159.80", cancelled.get("refund").decimalValue().toPlainString());
        JsonNode messages = json(call("GET", "/messages", null));
        assertEquals(before + 2, messages.size());
        assertEquals("Reserva cancelada - quarto 301", find(messages, "governanca@pousadamarealta.com.br").get("subject").asText());
        JsonNode fin = find(messages, "financeiro@pousadamarealta.com.br");
        assertEquals("Reembolso - reserva " + normal, fin.get("subject").asText());
        assertTrue(fin.get("body").asText().contains("R$ 1.159,80"));
        assertError(call("POST", "/reservations/" + normal + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");

        long strict = create(301, "2027-10-01", "2027-10-03", 2, "NON_REFUNDABLE");
        call("POST", "/reservations/" + strict + "/confirmation", null);
        before = json(call("GET", "/messages", null)).size();
        JsonNode c2 = json(call("POST", "/reservations/" + strict + "/cancellation", null));
        assertEquals("0.00", c2.get("refund").decimalValue().toPlainString());
        assertEquals(before + 1, json(call("GET", "/messages", null)).size()); // só governança

        long pending = create(301, "2027-11-01", "2027-11-03", 2, "STANDARD");
        before = json(call("GET", "/messages", null)).size();
        JsonNode c3 = json(call("POST", "/reservations/" + pending + "/cancellation", null));
        assertEquals("0.00", c3.get("refund").decimalValue().toPlainString());
        assertEquals(before, json(call("GET", "/messages", null)).size());
    }

    @Test
    void stayLifecycleAndDocument() throws Exception {
        long id = create(101, "2027-05-10", "2027-05-11", 1, "NON_REFUNDABLE");
        assertError(call("GET", "/reservations/" + id + "/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("POST", "/reservations/" + id + "/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());
        assertError(call("POST", "/reservations/" + id + "/check-out", null), 409, "OPERATION_NOT_ALLOWED");
        assertEquals("CHECKED_IN", json(call("POST", "/reservations/" + id + "/check-in", null)).get("status").asText());
        assertError(call("POST", "/reservations/" + id + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode doc = json(call("POST", "/reservations/" + id + "/check-out", null));
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals(id, doc.get("reservationId").asLong());
        assertEquals("101", doc.get("roomNumber").asText());
        assertEquals("STANDARD", doc.get("roomType").asText());
        assertEquals("212.42", doc.get("total").decimalValue().toPlainString());
        assertEquals("37.48", doc.get("discount").decimalValue().toPlainString());
        assertEquals(doc, json(call("GET", "/reservations/" + id + "/document", null)));
    }

    @Test
    void reports() throws Exception {
        long a = create(101, "2030-07-10", "2030-07-12", 2, "STANDARD");
        long b = create(102, "2030-07-09", "2030-07-10", 2, "STANDARD"); // sai no dia 10
        long c = create(201, "2030-07-10", "2030-07-11", 2, "NON_REFUNDABLE");
        long d = create(202, "2030-07-10", "2030-07-11", 2, "STANDARD"); // pendente
        call("POST", "/reservations/" + a + "/confirmation", null);
        call("POST", "/reservations/" + b + "/confirmation", null);
        call("POST", "/reservations/" + c + "/confirmation", null);
        call("POST", "/reservations/" + c + "/cancellation", null); // não reembolsável: fica com a pousada

        HttpResponse<String> occ = call("GET", "/reports/occupancy?date=2030-07-10", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse("").toLowerCase());
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD," + a + ",2030-07-10,2030-07-12\ntotal,1/5\n", occ.body());

        HttpResponse<String> rev = call("GET", "/reports/revenue?month=2030-07", null);
        assertEquals("text/csv; charset=utf-8", rev.headers().firstValue("Content-Type").orElse("").toLowerCase());
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + b + ",102,2030-07-09,2030-07-10,CONFIRMED,299.88\n"
                + a + ",101,2030-07-10,2030-07-12,CONFIRMED,599.76\n"
                + c + ",201,2030-07-10,2030-07-11,CANCELLED,397.70\n"
                + "total,1297.34\n", rev.body());
        assertTrue(d > 0);
    }
}
