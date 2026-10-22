package br.com.marealta.pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Teste de ponta a ponta contra o PostgreSQL de DB_URL, em um esquema temporário. Pulado sem banco. */
class ApiTest {
    static final ObjectMapper M = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static Database db;
    static Javalin app;
    static String schema;
    static String base;
    static String url, user, pass;

    static boolean testMode;

    static void start(boolean test) throws Exception {
        stop();
        testMode = test;
        schema = "t_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, pass); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        db = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, pass);
        db.migrate();
        var service = new ReservationService(db, new MessageDispatcher(Config.defaults(test), Clock.systemUTC()));
        app = new WebApp(service).create().start(0);
        base = "http://localhost:" + app.port();
    }

    static void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, pass); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
        app = null; db = null; schema = null;
    }

    @BeforeAll
    static void setup() {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        pass = System.getenv("DB_PASSWORD");
        Assumptions.assumeTrue(url != null, "DB_URL ausente");
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            // ok
        } catch (Exception e) {
            Assumptions.abort("Banco indisponível: " + e.getMessage());
        }
    }

    @AfterAll
    static void teardown() throws Exception { stop(); }

    @BeforeEach
    void fresh() throws Exception { start(false); }

    record Resp(int status, String body, String contentType) {
        JsonNode json() throws Exception { return M.readTree(body); }
    }

    Resp call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String body(int room, String in, String out, int guests, String plan, String email) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    JsonNode create(int room, String in, String out, int guests, String plan, String email) throws Exception {
        Resp r = call("POST", "/reservations", body(room, in, out, guests, plan, email));
        assertEquals(201, r.status(), r.body());
        return r.json();
    }

    @Test
    void rooms() throws Exception {
        JsonNode rooms = call("GET", "/rooms", null).json();
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
        assertEquals(4, rooms.get(4).get("capacity").asInt());
        assertEquals("579.90", rooms.get(4).get("nightlyRate").decimalValue().toPlainString());
    }

    @Test
    void createConfirmCheckInOutFlowAndMessages() throws Exception {
        JsonNode r = create(201, "2027-03-10", "2027-03-12", 2, "STANDARD", "carla@exemplo.com");
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("779.80", r.get("total").decimalValue().toPlainString());
        assertEquals("0.00", r.get("amountPaid").decimalValue().toPlainString());
        assertTrue(r.get("refund").isNull());
        assertEquals(409, call("GET", "/reservations/1/document", null).status());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).status());

        JsonNode c = call("POST", "/reservations/1/confirmation", null).json();
        assertEquals("CONFIRMED", c.get("status").asText());
        assertEquals("779.80", c.get("amountPaid").decimalValue().toPlainString());
        assertEquals(409, call("POST", "/reservations/1/confirmation", null).status());

        JsonNode msgs = call("GET", "/messages", null).json();
        assertEquals(2, msgs.size());
        assertEquals("SENT", msgs.get(0).get("status").asText());
        assertEquals("carla@exemplo.com", msgs.get(0).get("to").asText());
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
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", msgs.get(1).get("subject").asText());

    }

    @Test
    void checkInOutAndDocument() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 2, "NON_REFUNDABLE", "x@y.com");
        call("POST", "/reservations/1/confirmation", null);
        assertEquals(200, call("POST", "/reservations/1/check-in", null).status());
        assertEquals(409, call("POST", "/reservations/1/cancellation", null).status());
        Resp out = call("POST", "/reservations/1/check-out", null);
        assertEquals(200, out.status());
        JsonNode d = out.json();
        assertEquals("Comprovante de reserva", d.get("title").asText());
        assertEquals("37.48", d.get("discount").decimalValue().toPlainString());
        assertEquals("212.42", d.get("total").decimalValue().toPlainString());
        assertEquals("STANDARD", d.get("roomType").asText());
        assertEquals(d, call("GET", "/reservations/1/document", null).json());
        assertEquals(409, call("POST", "/reservations/1/check-out", null).status());
    }

    @Test
    void cancellationRefunds() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD", "x@y.com");
        create(102, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE", "x@y.com");
        create(201, "2027-03-10", "2027-03-11", 1, "STANDARD", "x@y.com");
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        assertEquals("249.90", call("POST", "/reservations/1/cancellation", null).json().get("refund").decimalValue().toPlainString());
        JsonNode nr = call("POST", "/reservations/2/cancellation", null).json();
        assertEquals("0.00", nr.get("refund").decimalValue().toPlainString());
        assertEquals("212.42", nr.get("amountPaid").decimalValue().toPlainString());
        JsonNode pending = call("POST", "/reservations/3/cancellation", null).json();
        assertEquals("0.00", pending.get("refund").decimalValue().toPlainString());
        assertEquals(409, call("POST", "/reservations/3/cancellation", null).status());
        assertEquals(409, call("POST", "/reservations/3/confirmation", null).status());
        assertEquals(409, call("GET", "/reservations/3/document", null).status());
    }

    @Test
    void validationAndErrors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "não é json").status());
        assertEquals(400, call("POST", "/reservations", "[]").status());
        assertEquals(400, call("POST", "/reservations", "{}").status());
        assertEquals(400, call("POST", "/reservations", body(101, "2027-03-10", "2027-03-10", 1, "STANDARD", "a@b.com")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "2027-03-10", "2027-03-09", 1, "STANDARD", "a@b.com")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "2027-03-10", "2027-03-11", 0, "STANDARD", "a@b.com")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "10/03/2027", "2027-03-11", 1, "STANDARD", "a@b.com")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "2027-03-10", "2027-03-11", 1, "VIP", "a@b.com")).status());
        Resp nf = call("POST", "/reservations", body(999, "2027-03-10", "2027-03-11", 1, "STANDARD", "a@b.com"));
        assertEquals(404, nf.status());
        assertEquals("ROOM_NOT_FOUND", nf.json().get("error").asText());
        Resp cap = call("POST", "/reservations", body(101, "2027-03-10", "2027-03-11", 3, "STANDARD", "a@b.com"));
        assertEquals(422, cap.status());
        assertEquals("CAPACITY_EXCEEDED", cap.json().get("error").asText());
        assertEquals("RESERVATION_NOT_FOUND", call("GET", "/reservations/42", null).json().get("error").asText());
        assertEquals(404, call("POST", "/reservations/42/confirmation", null).status());
        assertEquals(400, call("GET", "/reports/occupancy?date=10-07-2027", null).status());
        assertEquals(400, call("GET", "/reports/occupancy", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).status());
    }

    @Test
    void availability() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 1, "STANDARD", "a@b.com");
        Resp clash = call("POST", "/reservations", body(101, "2027-03-11", "2027-03-13", 1, "STANDARD", "a@b.com"));
        assertEquals(409, clash.status());
        assertEquals("ROOM_UNAVAILABLE", clash.json().get("error").asText());
        create(101, "2027-03-12", "2027-03-14", 1, "STANDARD", "a@b.com"); // sai no dia que outro entra
        call("POST", "/reservations/1/cancellation", null);
        create(101, "2027-03-10", "2027-03-12", 1, "STANDARD", "a@b.com"); // cancelada libera
    }

    @Test
    void invalidEmailIsBlockedAndNoCopy() throws Exception {
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD", "ana@gmail");
        assertEquals("CONFIRMED", call("POST", "/reservations/1/confirmation", null).json().get("status").asText());
        JsonNode msgs = call("GET", "/messages", null).json();
        assertEquals(1, msgs.size());
        assertEquals("BLOCKED", msgs.get(0).get("status").asText());
        assertEquals("ana@gmail", msgs.get(0).get("to").asText());
    }

    @Test
    void reports() throws Exception {
        create(301, "2027-07-10", "2027-07-12", 2, "STANDARD", "a@b.com");       // 1
        create(101, "2027-07-09", "2027-07-10", 1, "STANDARD", "a@b.com");       // 2 sai no dia 10
        create(102, "2027-07-10", "2027-07-11", 1, "NON_REFUNDABLE", "a@b.com"); // 3
        create(201, "2027-07-10", "2027-07-11", 1, "STANDARD", "a@b.com");       // 4 pendente
        create(202, "2027-07-20", "2027-07-21", 1, "STANDARD", "a@b.com");       // 5 cancelada com reembolso
        for (int i : new int[]{1, 2, 3, 5}) call("POST", "/reservations/" + i + "/confirmation", null);
        call("POST", "/reservations/5/cancellation", null);
        call("POST", "/reservations/3/cancellation", null);

        Resp occ = call("GET", "/reports/occupancy?date=2027-07-10", null);
        assertEquals("text/csv; charset=utf-8", occ.contentType().toLowerCase());
        assertEquals("room,type,reservation,check_in,check_out\n301,SUITE,1,2027-07-10,2027-07-12\ntotal,1/5\n", occ.body());

        Resp rev = call("GET", "/reports/revenue?month=2027-07", null);
        assertEquals("text/csv; charset=utf-8", rev.contentType().toLowerCase());
        // 1: 579.90*1.2=695.88 *2 = 1391.76 ; 2: 299.88 ; 3 (não reembolsável cancelada): 299.88-44.98 = 254.90 (44.982→44.98)
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + "2,101,2027-07-09,2027-07-10,CONFIRMED,299.88\n"
                + "1,301,2027-07-10,2027-07-12,CONFIRMED,1391.76\n"
                + "3,102,2027-07-10,2027-07-11,CANCELLED,254.90\n"
                + "total,1946.54\n", rev.body());
        assertEquals("reservation,room,check_in,check_out,status,amount\ntotal,0.00\n",
                call("GET", "/reports/revenue?month=2028-01", null).body());
    }

    @Test
    void testModeRedirectsMessages() throws Exception {
        start(true);
        create(101, "2027-03-10", "2027-03-11", 1, "STANDARD", "carla@exemplo.com");
        call("POST", "/reservations/1/confirmation", null);
        JsonNode msgs = call("GET", "/messages", null).json();
        assertEquals(1, msgs.size());
        assertEquals("testes@pousadamarealta.com.br", msgs.get(0).get("to").asText());
        assertTrue(msgs.get(0).get("subject").asText().startsWith("[TESTE] Reserva confirmada"));
        assertTrue(msgs.get(0).get("body").asText().startsWith("Destinatário original: carla@exemplo.com\nOlá, Carla Dias!"));
    }
}
