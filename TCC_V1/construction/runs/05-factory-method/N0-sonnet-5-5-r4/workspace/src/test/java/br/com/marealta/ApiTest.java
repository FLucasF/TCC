package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.*;

/** Usa um esquema temporário do banco indicado em DB_URL; é ignorado se o banco não estiver acessível. */
class ApiTest {
    static final ObjectMapper M = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String schema, url, user, pass;
    static HikariDataSource ds;
    static Javalin app;
    static String base;

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        pass = System.getenv("DB_PASSWORD");
        Assumptions.assumeTrue(url != null, "DB_URL ausente");
        schema = "it_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, pass); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            Assumptions.abort("Banco inacessível: " + e.getMessage());
        }
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        ds = new HikariDataSource(cfg);
        Db.init(ds);
        app = Api.create(new ReservationService(new Store(ds))).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (ds != null) ds.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, pass); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    record Res(int status, String body, String type) {
        JsonNode json() throws Exception { return M.readTree(body); }
    }

    static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String body(int room, String ident, String in, String out, int guests, String plan) {
        return """
            {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com",%s,
             "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, ident, in, out, guests, plan);
    }

    static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void fullLifecycleAndReports() throws Exception {
        Res rooms = call("GET", "/rooms", null);
        assertEquals(5, rooms.json().size());
        assertEquals(389.90, rooms.json().get(2).get("nightlyRate").asDouble());

        Res created = call("POST", "/reservations", body(201, CPF, "2031-03-10", "2031-03-12", 2, "STANDARD"));
        assertEquals(201, created.status());
        long id = created.json().get("id").asLong();
        assertEquals("PENDING", created.json().get("status").asText());
        assertTrue(created.body().contains("\"amountPaid\":0.00,\"refund\":null"), created.body());
        assertTrue(created.json().get("refund").isNull());
        assertTrue(created.json().get("companyCnpj").isNull());
        assertEquals(0, new java.math.BigDecimal("779.80").compareTo(created.json().get("total").decimalValue()));

        // conflito e capacidade
        assertEquals(409, call("POST", "/reservations", body(201, CPF, "2031-03-11", "2031-03-13", 2, "STANDARD")).status());
        assertEquals(422, call("POST", "/reservations", body(101, CPF, "2031-03-11", "2031-03-13", 3, "STANDARD")).status());
        // noites encostadas são permitidas
        assertEquals(201, call("POST", "/reservations", body(201, CPF, "2031-03-12", "2031-03-13", 1, "STANDARD")).status());

        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).status());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(0, new java.math.BigDecimal("779.80").compareTo(confirmed.json().get("amountPaid").decimalValue()));
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode msgs = call("GET", "/messages", null).json();
        assertEquals(1, msgs.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());
        assertTrue(msgs.get(0).get("body").asText().contains("Comprovante de reserva nº " + id));

        Res occ = call("GET", "/reports/occupancy?date=2031-03-11", null);
        assertTrue(occ.type().toLowerCase().contains("text/csv"));
        assertTrue(occ.type().toLowerCase().contains("utf-8"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2031-03-10,2031-03-12\ntotal,1/5\n", occ.body());
        assertTrue(call("GET", "/reports/occupancy?date=2031-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals(200, out.status());
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());

        Res rev = call("GET", "/reports/revenue?month=2031-03", null);
        assertEquals("reservation,room,check_in,check_out,status,amount\n" + id + ",201,2031-03-10,2031-03-12,CHECKED_OUT,779.80\ntotal,779.80\n", rev.body());
    }

    @Test
    void cancellationRefunds() throws Exception {
        Res a = call("POST", "/reservations", body(101, CPF, "2032-05-10", "2032-05-11", 1, "STANDARD"));
        long idA = a.json().get("id").asLong();
        call("POST", "/reservations/" + idA + "/confirmation", null);
        Res ca = call("POST", "/reservations/" + idA + "/cancellation", null);
        assertTrue(ca.body().contains("\"refund\":249.90"), ca.body());

        Res b = call("POST", "/reservations", body(102, CPF, "2032-05-10", "2032-05-11", 1, "NON_REFUNDABLE"));
        long idB = b.json().get("id").asLong();
        call("POST", "/reservations/" + idB + "/confirmation", null);
        Res cb = call("POST", "/reservations/" + idB + "/cancellation", null);
        assertTrue(cb.body().contains("\"refund\":0.00"), cb.body());

        Res c = call("POST", "/reservations", body(202, CPF, "2032-05-12", "2032-05-13", 1, "STANDARD"));
        long idC = c.json().get("id").asLong();
        assertTrue(call("POST", "/reservations/" + idC + "/cancellation", null).body().contains("\"refund\":0.00"));

        // quarto cancelado volta a ficar livre
        assertEquals(201, call("POST", "/reservations", body(101, CPF, "2032-05-10", "2032-05-11", 1, "STANDARD")).status());

        String rev = call("GET", "/reports/revenue?month=2032-05", null).body();
        assertTrue(rev.contains(idB + ",102,2032-05-10,2032-05-11,CANCELLED,212.42\n"), rev);
        assertFalse(rev.contains("\n" + idA + ","), rev);
        assertFalse(rev.contains("\n" + idC + ","), rev);
        assertTrue(rev.endsWith("total,212.42\n"), rev);
    }

    @Test
    void identificationVariants() throws Exception {
        String cnpj = "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"";
        Res co = call("POST", "/reservations", body(301, cnpj, "2033-03-10", "2033-03-12", 2, "STANDARD"));
        assertEquals(201, co.status());
        assertEquals("Tecnosul Ltda", co.json().get("companyName").asText());
        assertTrue(co.json().get("guestCpf").isNull());
        long idCo = co.json().get("id").asLong();
        call("POST", "/reservations/" + idCo + "/confirmation", null);
        JsonNode doc = call("GET", "/reservations/" + idCo + "/document", null).json();
        assertEquals("Fatura de hospedagem", doc.get("title").asText());
        assertTrue(doc.get("text").asText().endsWith("ISS retido (5%): R$ 57,99"), doc.get("text").asText());
        assertTrue(doc.get("guestPassport").isNull());

        String pass = "\"guestPassport\":\"X1234567\",\"guestCountry\":\"United Kingdom\"";
        Res p = call("POST", "/reservations", body(301, pass, "2033-04-10", "2033-04-11", 1, "STANDARD"));
        long idP = p.json().get("id").asLong();
        call("POST", "/reservations/" + idP + "/confirmation", null);
        JsonNode pd = call("GET", "/reservations/" + idP + "/document", null).json();
        assertEquals("Booking receipt", pd.get("title").asText());
        assertTrue(pd.get("text").asText().contains("passport X1234567, United Kingdom"));
        // e-mail continua em português
        JsonNode msgs = call("GET", "/messages", null).json();
        String last = msgs.get(msgs.size() - 1).get("body").asText();
        assertTrue(last.startsWith("Olá, Carla Dias!"));
        assertTrue(last.contains("Booking receipt no. " + idP));
    }

    @Test
    void validation() throws Exception {
        String ok = body(101, CPF, "2034-03-10", "2034-03-12", 2, "STANDARD");
        assertEquals(400, call("POST", "/reservations", "not json").status());
        assertEquals(400, call("POST", "/reservations", "[]").status());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2034-03-12", "2034-03-12", 2, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2034-3-10", "2034-03-12", 2, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2034-03-10", "2034-03-12", 0, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2034-03-10", "2034-03-12", 2, "OTHER")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "\"companyCnpj\":\"1\"", "2034-03-10", "2034-03-12", 2, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "\"guestCpf\":\"1\",\"guestPassport\":\"2\",\"guestCountry\":\"BR\"", "2034-03-10", "2034-03-12", 2, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "\"guestName2\":\"x\"", "2034-03-10", "2034-03-12", 2, "STANDARD")).status());
        assertEquals(404, call("POST", "/reservations", body(999, CPF, "2034-03-10", "2034-03-12", 2, "STANDARD")).status());
        assertEquals("ROOM_NOT_FOUND", call("POST", "/reservations", body(999, CPF, "2034-03-10", "2034-03-12", 2, "STANDARD")).json().get("error").asText());
        assertEquals(404, call("GET", "/reservations/99999", null).status());
        assertEquals(404, call("GET", "/reservations/abc", null).status());
        assertEquals(400, call("GET", "/reports/occupancy?date=2034-13-01", null).status());
        assertEquals(400, call("GET", "/reports/revenue", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2034-7", null).status());
        assertEquals(201, call("POST", "/reservations", ok).status());
    }

    @Test
    void concurrentBookingsOfSameRoomOnlyOneWins() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
        for (int i = 0; i < 8; i++) {
            futures.add(pool.submit(() -> call("POST", "/reservations", body(202, CPF, "2035-03-10", "2035-03-12", 2, "STANDARD")).status()));
        }
        int created = 0;
        for (var f : futures) if (f.get() == 201) created++;
        pool.shutdown();
        assertEquals(1, created);
    }
}
