package pousada;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Teste de ponta a ponta; usa um schema próprio dentro do banco de DB_URL e o remove no fim. */
class ApiTest {
    static final ObjectMapper M = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String schema;
    static String url, user, password;
    static Db db;
    static Javalin app;
    static String base;

    record Resp(int status, String body, String contentType) {
        JsonNode json() throws Exception { return M.readTree(body); }
    }

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        assumeTrue(url != null, "DB_URL ausente");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, password); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            assumeTrue(false, "banco indisponível: " + e.getMessage());
        }
        db = new Db(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        db.init();
        app = new App(new Service(db)).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, password); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    static Resp call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody());
        else b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String req(int room, String id, int guests, String plan, String in, String out) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com",%s,
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, id, in, out, guests, plan);
    }

    static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    static void eq(String expected, JsonNode n) {
        assertEquals(0, new java.math.BigDecimal(expected).compareTo(n.decimalValue()), n.toString());
    }

    static JsonNode create(String body) throws Exception {
        Resp r = call("POST", "/reservations", body);
        assertEquals(201, r.status(), r.body());
        return r.json();
    }

    @Test
    void rooms() throws Exception {
        JsonNode j = call("GET", "/rooms", null).json();
        assertEquals(5, j.size());
        assertTrue(call("GET", "/rooms", null).body().contains("\"nightlyRate\":249.90"));
        assertEquals(101, j.get(0).get("number").asInt());
        eq("249.90", j.get(0).get("nightlyRate"));
    }

    @Test
    void fullLifecycleWithCpf() throws Exception {
        JsonNode r = create(req(201, CPF, 2, "STANDARD", "2030-03-10", "2030-03-12"));
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(call("GET", "/reservations/" + id, null).body().contains("\"total\":779.80,\"status\":\"PENDING\",\"amountPaid\":0.00,\"refund\":null"));
        eq("779.80", r.get("total"));
        assertEquals(0, r.get("amountPaid").decimalValue().signum());
        assertTrue(r.get("refund").isNull());
        assertTrue(r.get("companyCnpj").isNull());

        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).status());

        JsonNode c = call("POST", "/reservations/" + id + "/confirmation", null).json();
        assertEquals("CONFIRMED", c.get("status").asText());
        eq("779.80", c.get("amountPaid"));
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode msgs = call("GET", "/messages", null).json();
        JsonNode m = msgs.get(msgs.size() - 1);
        assertEquals("EMAIL", m.get("channel").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.get("subject").asText());
        assertTrue(m.get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada."));
        assertTrue(m.get("body").asText().contains("Comprovante de reserva nº " + id + "\nHóspede: Carla Dias (CPF 111.222.333-44)"));
        assertTrue(m.get("body").asText().endsWith("Total: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"));

        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-out", null).status());
        assertEquals("CHECKED_IN", call("POST", "/reservations/" + id + "/check-in", null).json().get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());
        JsonNode doc = call("POST", "/reservations/" + id + "/check-out", null).json();
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("111.222.333-44", doc.get("guestCpf").asText());
        assertTrue(doc.get("guestPassport").isNull());
        eq("779.80", doc.get("total"));
        assertTrue(doc.get("text").asText().startsWith("Comprovante de reserva nº " + id));
    }

    @Test
    void companyAndForeignerDocuments() throws Exception {
        JsonNode a = create(req(202, "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"", 1, "STANDARD", "2030-03-10", "2030-03-12"));
        call("POST", "/reservations/" + a.get("id").asLong() + "/confirmation", null);
        JsonNode da = call("GET", "/reservations/" + a.get("id").asLong() + "/document", null).json();
        assertEquals("Fatura de hospedagem", da.get("title").asText());
        assertTrue(da.get("text").asText().contains("Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)"));
        assertTrue(da.get("text").asText().endsWith("ISS retido (5%): R$ 38,99"));

        JsonNode b = create(req(202, "\"guestPassport\":\"X1234567\",\"guestCountry\":\"United Kingdom\"", 1, "NON_REFUNDABLE", "2030-04-10", "2030-04-11"));
        long bid = b.get("id").asLong();
        eq("58.48", b.get("discount")); // 15% de 389.90 = 58.485 -> 58.48 (par)
        call("POST", "/reservations/" + bid + "/confirmation", null);
        JsonNode db_ = call("GET", "/reservations/" + bid + "/document", null).json();
        assertEquals("Booking receipt", db_.get("title").asText());
        assertTrue(db_.get("text").asText().contains("Guest: Carla Dias (passport X1234567, United Kingdom)"));
        assertTrue(db_.get("text").asText().contains("| 1 night\nNights: BRL 389.90\nDiscount: BRL 58.48\nTotal: BRL 331.42"));
    }

    @Test
    void cancellationRefunds() throws Exception {
        JsonNode std = create(req(101, CPF, 2, "STANDARD", "2030-05-01", "2030-05-03"));
        long s = std.get("id").asLong();
        call("POST", "/reservations/" + s + "/confirmation", null);
        JsonNode cs = call("POST", "/reservations/" + s + "/cancellation", null).json();
        assertEquals("CANCELLED", cs.get("status").asText());
        assertEquals(cs.get("amountPaid").decimalValue().toPlainString(), cs.get("refund").decimalValue().toPlainString());
        assertEquals(409, call("GET", "/reservations/" + s + "/document", null).status());

        JsonNode nr = create(req(101, CPF, 2, "NON_REFUNDABLE", "2030-05-01", "2030-05-03")); // noites liberadas
        long n = nr.get("id").asLong();
        call("POST", "/reservations/" + n + "/confirmation", null);
        assertEquals(0, call("POST", "/reservations/" + n + "/cancellation", null).json().get("refund").decimalValue().signum());

        JsonNode pend = create(req(101, CPF, 2, "STANDARD", "2030-05-01", "2030-05-03"));
        JsonNode cp = call("POST", "/reservations/" + pend.get("id").asLong() + "/cancellation", null).json();
        assertEquals(0, cp.get("refund").decimalValue().signum());

        // reembolsável não reembolsada conta no financeiro
        Resp rev = call("GET", "/reports/revenue?month=2030-05", null);
        assertTrue(rev.contentType().toLowerCase().contains("text/csv") && rev.contentType().toLowerCase().contains("utf-8"));
        assertTrue(rev.body().contains("\n" + n + ",101,2030-05-01,2030-05-03,CANCELLED,424.83\n"), rev.body());
        assertFalse(rev.body().contains("\n" + s + ","));
        assertTrue(rev.body().endsWith("total,424.83\n"), rev.body());
    }

    @Test
    void validationAndConflicts() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao e json").status());
        assertEquals(400, call("POST", "/reservations", req(101, CPF, 2, "STANDARD", "2030-06-12", "2030-06-12")).status());
        assertEquals(400, call("POST", "/reservations", req(101, CPF, 0, "STANDARD", "2030-06-10", "2030-06-12")).status());
        assertEquals(400, call("POST", "/reservations", req(101, CPF, 2, "STANDARD", "2030-6-10", "2030-06-12")).status());
        assertEquals(400, call("POST", "/reservations", req(101, "\"companyCnpj\":\"1\"", 2, "STANDARD", "2030-06-10", "2030-06-12")).status());
        assertEquals(400, call("POST", "/reservations", req(101, CPF + ",\"guestPassport\":\"X\",\"guestCountry\":\"BR\"", 2, "STANDARD", "2030-06-10", "2030-06-12")).status());
        assertEquals(400, call("POST", "/reservations", req(101, "\"x\":1", 2, "STANDARD", "2030-06-10", "2030-06-12")).status());
        assertEquals("ROOM_NOT_FOUND", call("POST", "/reservations", req(999, CPF, 2, "STANDARD", "2030-06-10", "2030-06-12")).json().get("error").asText());
        Resp cap = call("POST", "/reservations", req(101, CPF, 3, "STANDARD", "2030-06-10", "2030-06-12"));
        assertEquals(422, cap.status());
        assertEquals("CAPACITY_EXCEEDED", cap.json().get("error").asText());

        create(req(301, CPF, 4, "STANDARD", "2030-06-10", "2030-06-12"));
        Resp clash = call("POST", "/reservations", req(301, CPF, 1, "STANDARD", "2030-06-11", "2030-06-13"));
        assertEquals(409, clash.status());
        assertEquals("ROOM_UNAVAILABLE", clash.json().get("error").asText());
        create(req(301, CPF, 1, "STANDARD", "2030-06-12", "2030-06-13")); // saída no dia da entrada não conflita

        assertEquals("RESERVATION_NOT_FOUND", call("GET", "/reservations/99999", null).json().get("error").asText());
        assertEquals(404, call("GET", "/reservations/abc", null).status());
        assertEquals(400, call("GET", "/reports/occupancy?date=2030-13-01", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2030-7", null).status());
    }

    @Test
    void occupancyReport() throws Exception {
        long a = create(req(102, CPF, 2, "STANDARD", "2031-01-10", "2031-01-12")).get("id").asLong();
        long b = create(req(201, CPF, 2, "STANDARD", "2031-01-11", "2031-01-13")).get("id").asLong();
        create(req(202, CPF, 2, "STANDARD", "2031-01-11", "2031-01-13")); // pendente: não conta
        call("POST", "/reservations/" + a + "/confirmation", null);
        call("POST", "/reservations/" + b + "/confirmation", null);

        assertEquals("room,type,reservation,check_in,check_out\n102,STANDARD," + a + ",2031-01-10,2031-01-12\n"
                + "201,DELUXE," + b + ",2031-01-11,2031-01-13\ntotal,2/5\n",
                call("GET", "/reports/occupancy?date=2031-01-11", null).body());
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + b + ",2031-01-11,2031-01-13\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2031-01-12", null).body());
    }
}
