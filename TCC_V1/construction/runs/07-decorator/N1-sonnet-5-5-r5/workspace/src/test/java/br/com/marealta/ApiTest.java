package br.com.marealta;

import br.com.marealta.db.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Teste de ponta a ponta contra o PostgreSQL de DB_URL, num esquema descartável. */
class ApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static AppConfig env;
    private static Database database;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        env = AppConfig.fromEnv(System.getenv());
        Assumptions.assumeTrue(env.dbUrl() != null, "DB_URL não definido");
        schema = "teste_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(env.dbUrl(), env.dbUser(), env.dbPassword());
             Statement s = c.createStatement()) {
            s.execute("create schema " + schema);
        }
        database = new Database(env.dbUrl(), env.dbUser(), env.dbPassword(), schema);
        app = App.start(new AppConfig(0, env.dbUrl(), env.dbUser(), env.dbPassword(), false, null), database);
        app.start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (database != null) database.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(env.dbUrl(), env.dbUser(), env.dbPassword());
                 Statement s = c.createStatement()) {
                s.execute("drop schema " + schema + " cascade");
            }
        }
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan, String email) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    private static JsonNode create(int room, String in, String out, int guests, String plan, String email) throws Exception {
        HttpResponse<String> r = call("POST", "/reservations", booking(room, in, out, guests, plan, email));
        assertEquals(201, r.statusCode(), r.body());
        return MAPPER.readTree(r.body());
    }

    private static void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, MAPPER.readTree(r.body()).get("error").asText());
    }

    @Test
    void fullFlow() throws Exception {
        JsonNode rooms = MAPPER.readTree(call("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(389.90, rooms.get(2).get("nightlyRate").asDouble());

        JsonNode r = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD", "carla@exemplo.com");
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("779.80", r.get("total").decimalValue().toPlainString());
        assertEquals("0.00", r.get("amountPaid").decimalValue().toPlainString());
        assertTrue(r.get("refund").isNull());

        assertError(call("POST", "/reservations/" + id + "/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(call("GET", "/reservations/" + id + "/document", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode confirmed = MAPPER.readTree(call("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("779.80", confirmed.get("amountPaid").decimalValue().toPlainString());

        JsonNode messages = MAPPER.readTree(call("GET", "/messages", null).body());
        int at = 0;
        while (!messages.get(at).get("body").asText().contains("nº " + id + "\n")) at++;
        messages = MAPPER.createArrayNode().add(messages.get(at)).add(messages.get(at + 1));
        assertEquals("carla@exemplo.com", messages.get(0).get("to").asText());
        assertEquals("SENT", messages.get(0).get("status").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
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
                Até breve!""".formatted(id), messages.get(0).get("body").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", messages.get(1).get("subject").asText());

        HttpResponse<String> occupancy = call("GET", "/reports/occupancy?date=2027-03-10", null);
        assertTrue(occupancy.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertTrue(occupancy.body().startsWith("room,type,reservation,check_in,check_out\n201,DELUXE," + id));
        assertTrue(occupancy.body().endsWith("total,1/5\n"));
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));

        call("POST", "/reservations/" + id + "/check-in", null);
        JsonNode receipt = MAPPER.readTree(call("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());
    }

    @Test
    void cancellationAndRevenue() throws Exception {
        long nonRefundable = create(301, "2028-05-10", "2028-05-11", 4, "NON_REFUNDABLE", "a@b.com").get("id").asLong();
        call("POST", "/reservations/" + nonRefundable + "/confirmation", null);
        JsonNode cancelled = MAPPER.readTree(call("POST", "/reservations/" + nonRefundable + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals("0.00", cancelled.get("refund").decimalValue().toPlainString());

        long pending = create(301, "2028-05-12", "2028-05-13", 1, "STANDARD", "a@b.com").get("id").asLong();
        assertEquals("0.00", MAPPER.readTree(call("POST", "/reservations/" + pending + "/cancellation", null).body())
                .get("refund").decimalValue().toPlainString());
        assertError(call("POST", "/reservations/" + pending + "/cancellation", null), 409, "OPERATION_NOT_ALLOWED");

        long refundable = create(301, "2028-05-20", "2028-05-21", 1, "STANDARD", "a@b.com").get("id").asLong();
        call("POST", "/reservations/" + refundable + "/confirmation", null);
        call("POST", "/reservations/" + refundable + "/cancellation", null);

        String revenue = call("GET", "/reports/revenue?month=2028-05", null).body();
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + nonRefundable + ",301,2028-05-10,2028-05-11,CANCELLED,492.92\n"
                + "total,492.92\n", revenue);
    }

    @Test
    void invalidEmailIsBlocked() throws Exception {
        long id = create(102, "2029-01-10", "2029-01-11", 1, "STANDARD", "ana@gmail").get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode messages = MAPPER.readTree(call("GET", "/messages", null).body());
        JsonNode last = messages.get(messages.size() - 1);
        assertEquals("BLOCKED", last.get("status").asText());
        assertEquals("ana@gmail", last.get("to").asText());
    }

    @Test
    void rejections() throws Exception {
        create(101, "2030-04-10", "2030-04-12", 2, "STANDARD", "a@b.com");
        assertError(call("POST", "/reservations", booking(101, "2030-04-11", "2030-04-13", 2, "STANDARD", "a@b.com")), 409, "ROOM_UNAVAILABLE");
        create(101, "2030-04-12", "2030-04-13", 2, "STANDARD", "a@b.com");
        assertError(call("POST", "/reservations", booking(101, "2030-05-11", "2030-05-13", 3, "STANDARD", "a@b.com")), 422, "CAPACITY_EXCEEDED");
        assertError(call("POST", "/reservations", booking(999, "2030-05-11", "2030-05-13", 1, "STANDARD", "a@b.com")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(101, "2030-05-13", "2030-05-13", 1, "STANDARD", "a@b.com")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2030-05-11", "2030-05-13", 0, "STANDARD", "a@b.com")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{nope"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reservations/99999", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reports/occupancy?date=10-07-2027", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue?month=2027-13", null), 400, "INVALID_REQUEST");
    }
}
