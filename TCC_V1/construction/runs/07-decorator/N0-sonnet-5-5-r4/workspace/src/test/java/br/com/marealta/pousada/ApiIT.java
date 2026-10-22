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

/** Roda contra o PostgreSQL de DB_URL, num schema próprio que é apagado no final. */
class ApiIT {
    static final ObjectMapper M = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String schema, baseUrl, user, pass, url;
    static Db db;
    static Javalin app;

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "DB_URL não definida");
        user = System.getenv("DB_USER");
        pass = System.getenv("DB_PASSWORD");
        schema = "it_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, pass); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        String scoped = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        db = new Db(scoped, user, pass);
        db.init();
        var mail = new MailDispatcher(MailConfig.defaults(false), Clock.systemUTC());
        app = new Api(new ReservationService(db, mail)).build().start(0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, pass); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    record Resp(int status, String body, String contentType) {
        JsonNode json() throws Exception { return M.readTree(body); }
    }

    static Resp call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(baseUrl + path));
        b.header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String req(int room, String email, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        var rooms = call("GET", "/rooms", null).json();
        assertEquals(5, rooms.size());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble());

        Resp c = call("POST", "/reservations", req(201, "carla@email.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, c.status());
        JsonNode r = c.json();
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(c.body().contains("\"total\":779.80"), c.body());
        assertTrue(r.get("refund").isNull());

        // sem comprovante antes do pagamento
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        // sobreposição
        Resp dup = call("POST", "/reservations", req(201, "x@email.com", "2027-03-11", "2027-03-13", 1, "STANDARD"));
        assertEquals(409, dup.status());
        assertEquals("ROOM_UNAVAILABLE", dup.json().get("error").asText());
        // noite da saída pode ser reservada
        assertEquals(201, call("POST", "/reservations", req(201, "x@email.com", "2027-03-12", "2027-03-13", 1, "STANDARD")).status());

        Resp conf = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, conf.status());
        assertEquals("CONFIRMED", conf.json().get("status").asText());
        assertTrue(conf.body().contains("\"amountPaid\":779.80"), conf.body());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode msgs = call("GET", "/messages", null).json();
        JsonNode all = msgs;
        msgs = M.createArrayNode().add(all.get(all.size() - 2)).add(all.get(all.size() - 1));
        assertEquals("carla@email.com", msgs.get(0).get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("SENT", msgs.get(0).get("status").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", msgs.get(1).get("subject").asText());
        String expectedBody = """
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº %d
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""".formatted(id);
        assertEquals(expectedBody, msgs.get(0).get("body").asText());

        Resp rep = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertEquals("text/csv; charset=utf-8", rep.contentType().toLowerCase());
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2027-03-10,2027-03-12\ntotal,1/5\n", rep.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().contains("total,0/5") ||
                call("GET", "/reports/occupancy?date=2027-03-12", null).body().contains("total,1/5"));

        assertEquals(409, call("POST", "/reservations/" + id + "/check-out", null).status());
        assertEquals("CHECKED_IN", call("POST", "/reservations/" + id + "/check-in", null).json().get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());
        Resp doc = call("GET", "/reservations/" + id + "/document", null);
        assertEquals(200, doc.status());
        Resp out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals("DELUXE", out.json().get("roomType").asText());
        assertEquals("2027-03-10", out.json().get("checkIn").asText());

        Resp fin = call("GET", "/reports/revenue?month=2027-03", null);
        assertTrue(fin.body().startsWith("reservation,room,check_in,check_out,status,amount\n" + id + ",201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\n"), fin.body());
    }

    @Test
    void cancellationsAndRefunds() throws Exception {
        // não reembolsável confirmada: sem devolução, continua no financeiro
        long a = call("POST", "/reservations", req(301, "a@x.com", "2028-05-01", "2028-05-03", 4, "NON_REFUNDABLE")).json().get("id").asLong();
        JsonNode ra = call("GET", "/reservations/" + a, null).json();
        assertEquals(1159.80, ra.get("subtotal").asDouble());
        assertEquals(173.97, ra.get("discount").asDouble());
        assertEquals(985.83, ra.get("total").asDouble());
        call("POST", "/reservations/" + a + "/confirmation", null);
        JsonNode ca = call("POST", "/reservations/" + a + "/cancellation", null).json();
        assertEquals("CANCELLED", ca.get("status").asText());
        assertEquals(0.0, ca.get("refund").asDouble());
        // a data liberou-se
        long b = call("POST", "/reservations", req(301, "b@x.com", "2028-05-01", "2028-05-03", 1, "STANDARD")).json().get("id").asLong();
        // padrão confirmada: devolve tudo
        call("POST", "/reservations/" + b + "/confirmation", null);
        JsonNode cb = call("POST", "/reservations/" + b + "/cancellation", null).json();
        assertEquals(1159.80, cb.get("refund").asDouble());
        // pendente: nada a devolver
        long c = call("POST", "/reservations", req(301, "c@x.com", "2028-05-01", "2028-05-03", 1, "STANDARD")).json().get("id").asLong();
        assertEquals(0.0, call("POST", "/reservations/" + c + "/cancellation", null).json().get("refund").asDouble());
        assertEquals(409, call("POST", "/reservations/" + c + "/cancellation", null).status());

        String rev = call("GET", "/reports/revenue?month=2028-05", null).body();
        assertTrue(rev.contains(a + ",301,2028-05-01,2028-05-03,CANCELLED,985.83\n"), rev);
        assertFalse(rev.contains("\n" + b + ","), rev);
        assertTrue(rev.endsWith("total,985.83\n"), rev);
    }

    @Test
    void invalidEmailIsBlocked() throws Exception {
        long id = call("POST", "/reservations", req(102, "ana@gmail", "2029-01-10", "2029-01-11", 1, "STANDARD")).json().get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode msgs = call("GET", "/messages", null).json();
        JsonNode last = msgs.get(msgs.size() - 1);
        assertEquals("ana@gmail", last.get("to").asText());
        assertEquals("BLOCKED", last.get("status").asText());
        assertEquals("1 diária", "1 diária");
        assertTrue(last.get("body").asText().contains("| 1 diária\n"));
    }

    @Test
    void errors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao json").status());
        assertEquals(400, call("POST", "/reservations", "{}").status());
        assertEquals(400, call("POST", "/reservations", req(101, "a@b.com", "2027-03-10", "2027-03-10", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", req(101, "a@b.com", "2027-03-10", "2027-03-11", 0, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", req(101, "a@b.com", "10/03/2027", "2027-03-11", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", req(101, "a@b.com", "2027-02-30", "2027-03-11", 1, "STANDARD")).status());
        Resp nr = call("POST", "/reservations", req(999, "a@b.com", "2027-03-10", "2027-03-11", 1, "STANDARD"));
        assertEquals(404, nr.status());
        assertEquals("ROOM_NOT_FOUND", nr.json().get("error").asText());
        Resp cap = call("POST", "/reservations", req(101, "a@b.com", "2027-03-10", "2027-03-11", 3, "STANDARD"));
        assertEquals(422, cap.status());
        assertEquals("CAPACITY_EXCEEDED", cap.json().get("error").asText());
        assertEquals("RESERVATION_NOT_FOUND", call("GET", "/reservations/99999", null).json().get("error").asText());
        assertEquals(404, call("POST", "/reservations/abc/check-in", null).status());
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-3-1", null).status());
        assertEquals(400, call("GET", "/reports/occupancy", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).status());
    }
}
