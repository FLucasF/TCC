package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Testa a API de ponta a ponta contra um PostgreSQL de verdade (embutido). */
class ApiIT {
    static EmbeddedPostgres pg;
    static final ObjectMapper M = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(com.fasterxml.jackson.databind.cfg.JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);
    static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        try {
            pg = EmbeddedPostgres.builder().start();
        } catch (Exception | Error e) {
            System.err.println("PostgreSQL embutido indisponível, testes de integração ignorados: " + e);
        }
        assumeTrue(pg != null, "PostgreSQL embutido indisponível");
    }

    @AfterAll
    static void stop() throws Exception {
        if (pg != null) pg.close();
    }

    record Env(Javalin app, String base, Database db) implements AutoCloseable {
        public void close() {
            app.stop();
            db.close();
        }
    }

    static int dbCounter = 0;

    static Env env(Map<String, String> vars, Clock clock) throws Exception {
        String name = "t" + (++dbCounter);
        try (Connection c = pg.getPostgresDatabase().getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + name);
        }
        String url = pg.getJdbcUrl("postgres", name);
        Config base = Config.fromEnv(vars);
        Database db = new Database(url, "postgres", "postgres");
        db.migrate();
        db.migrate(); // idempotente
        Javalin app = Main.create(new Service(db, clock, base)).start(0);
        return new Env(app, "http://localhost:" + app.port(), db);
    }

    static Clock noon() {
        return Clock.fixed(Instant.parse("2027-03-01T15:00:00Z"), ZoneOffset.UTC); // 12h em São Paulo
    }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception { return M.readTree(body); }
    }

    static Res call(Env e, String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(e.base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String booking(int room, String email, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    @Test
    void roomsAndFullLifecycle() throws Exception {
        try (Env e = env(Map.of(), noon())) {
            JsonNode rooms = call(e, "GET", "/rooms", null).json();
            assertEquals(5, rooms.size());
            assertEquals(101, rooms.get(0).get("number").asInt());
            assertEquals(579.90, rooms.get(4).get("nightlyRate").asDouble());

            Res created = call(e, "POST", "/reservations", booking(201, "carla@x.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            assertEquals(201, created.status);
            JsonNode r = created.json();
            assertEquals(1, r.get("id").asInt());
            assertEquals("PENDING", r.get("status").asText());
            assertEquals("779.80", r.get("subtotal").toString());
            assertEquals("0.00", r.get("amountPaid").toString());
            assertTrue(r.get("refund").isNull());

            assertEquals(409, call(e, "GET", "/reservations/1/document", null).status);
            assertEquals(409, call(e, "POST", "/reservations/1/check-in", null).status);
            assertEquals(0, call(e, "GET", "/messages", null).json().size());

            JsonNode conf = call(e, "POST", "/reservations/1/confirmation", null).json();
            assertEquals("CONFIRMED", conf.get("status").asText());
            assertEquals("779.80", conf.get("amountPaid").toString());
            assertEquals(409, call(e, "POST", "/reservations/1/confirmation", null).status);

            JsonNode msgs = call(e, "GET", "/messages", null).json();
            assertEquals(2, msgs.size());
            assertEquals("carla@x.com", msgs.get(0).get("to").asText());
            assertEquals("SENT", msgs.get(0).get("status").asText());
            assertEquals("EMAIL", msgs.get(0).get("channel").asText());
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
            assertEquals("arquivo@pousadamarealta.com.br", msgs.get(1).get("to").asText());
            assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", msgs.get(1).get("subject").asText());

            assertEquals(200, call(e, "GET", "/reservations/1/document", null).status);
            assertEquals(409, call(e, "POST", "/reservations/1/check-out", null).status);
            assertEquals("CHECKED_IN", call(e, "POST", "/reservations/1/check-in", null).json().get("status").asText());
            assertEquals(409, call(e, "POST", "/reservations/1/cancellation", null).status);
            Res out = call(e, "POST", "/reservations/1/check-out", null);
            assertEquals(200, out.status);
            JsonNode doc = out.json();
            assertEquals("Comprovante de reserva", doc.get("title").asText());
            assertEquals("DELUXE", doc.get("roomType").asText());
            assertEquals("779.80", doc.get("total").toString());
            assertEquals(doc, call(e, "GET", "/reservations/1/document", null).json());
        }
    }

    @Test
    void validationAndConflicts() throws Exception {
        try (Env e = env(Map.of(), noon())) {
            assertEquals(400, call(e, "POST", "/reservations", "nao json").status);
            assertEquals(400, call(e, "POST", "/reservations", "").status);
            assertEquals(400, call(e, "POST", "/reservations", "{}").status);
            assertEquals(400, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-10", 1, "STANDARD")).status);
            assertEquals(400, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-09", 1, "STANDARD")).status);
            assertEquals(400, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-11", 0, "STANDARD")).status);
            assertEquals(400, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-02-30", "2027-03-11", 1, "STANDARD")).status);
            assertEquals(400, call(e, "POST", "/reservations", booking(101, "a@b.com", "10/03/2027", "2027-03-11", 1, "STANDARD")).status);
            assertEquals(400, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-11", 1, "VIP")).status);
            Res nf = call(e, "POST", "/reservations", booking(999, "a@b.com", "2027-03-10", "2027-03-11", 1, "STANDARD"));
            assertEquals(404, nf.status);
            assertEquals("ROOM_NOT_FOUND", nf.json().get("error").asText());
            Res cap = call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-11", 3, "STANDARD"));
            assertEquals(422, cap.status);
            assertEquals("CAPACITY_EXCEEDED", cap.json().get("error").asText());

            assertEquals(201, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD")).status);
            Res clash = call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-11", "2027-03-13", 2, "STANDARD"));
            assertEquals(409, clash.status);
            assertEquals("ROOM_UNAVAILABLE", clash.json().get("error").asText());
            // sair no dia em que o outro entra é permitido
            assertEquals(201, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-12", "2027-03-13", 2, "STANDARD")).status);
            // cancelada libera
            assertEquals(200, call(e, "POST", "/reservations/1/cancellation", null).status);
            assertEquals(201, call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD")).status);

            assertEquals("RESERVATION_NOT_FOUND", call(e, "GET", "/reservations/77", null).json().get("error").asText());
            assertEquals(404, call(e, "GET", "/reservations/abc", null).status);
            assertEquals(400, call(e, "GET", "/reports/occupancy?date=2027-3-1", null).status);
            assertEquals(400, call(e, "GET", "/reports/occupancy", null).status);
            assertEquals(400, call(e, "GET", "/reports/revenue?month=2027-13", null).status);
        }
    }

    @Test
    void cancellationRefunds() throws Exception {
        try (Env e = env(Map.of(), noon())) {
            // pendente: nada a devolver
            call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            JsonNode c1 = call(e, "POST", "/reservations/1/cancellation", null).json();
            assertEquals("CANCELLED", c1.get("status").asText());
            assertEquals("0.00", c1.get("refund").toString());
            assertEquals(409, call(e, "POST", "/reservations/1/cancellation", null).status);
            assertEquals(409, call(e, "POST", "/reservations/1/confirmation", null).status);
            assertEquals(409, call(e, "GET", "/reservations/1/document", null).status);

            // normal confirmada: devolve tudo
            call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            call(e, "POST", "/reservations/2/confirmation", null);
            JsonNode c2 = call(e, "POST", "/reservations/2/cancellation", null).json();
            assertEquals("499.80", c2.get("refund").toString());
            assertEquals("499.80", c2.get("amountPaid").toString());

            // não reembolsável confirmada: não devolve
            call(e, "POST", "/reservations", booking(102, "a@b.com", "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
            JsonNode r3 = call(e, "POST", "/reservations/3/confirmation", null).json();
            assertEquals("424.83", r3.get("total").toString());
            assertEquals("74.97", r3.get("discount").toString());
            JsonNode c3 = call(e, "POST", "/reservations/3/cancellation", null).json();
            assertEquals("0.00", c3.get("refund").toString());

            // relatório financeiro: só a não reembolsável cancelada deixou dinheiro
            Res rev = call(e, "GET", "/reports/revenue?month=2027-03", null);
            assertEquals("text/csv; charset=utf-8", rev.contentType.toLowerCase());
            assertEquals("reservation,room,check_in,check_out,status,amount\n3,102,2027-03-10,2027-03-12,CANCELLED,424.83\ntotal,424.83\n", rev.body);
        }
    }

    @Test
    void reports() throws Exception {
        try (Env e = env(Map.of(), noon())) {
            call(e, "POST", "/reservations", booking(301, "a@b.com", "2027-07-10", "2027-07-12", 2, "STANDARD")); // 1
            call(e, "POST", "/reservations", booking(101, "a@b.com", "2027-07-11", "2027-07-13", 2, "STANDARD")); // 2
            call(e, "POST", "/reservations", booking(102, "a@b.com", "2027-07-05", "2027-07-11", 2, "STANDARD")); // 3
            call(e, "POST", "/reservations", booking(201, "a@b.com", "2027-07-11", "2027-07-12", 2, "STANDARD")); // 4 pendente
            for (int i = 1; i <= 3; i++) call(e, "POST", "/reservations/" + i + "/confirmation", null);

            Res occ = call(e, "GET", "/reports/occupancy?date=2027-07-11", null);
            assertEquals(200, occ.status);
            assertTrue(occ.contentType.toLowerCase().contains("text/csv"));
            assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD,2,2027-07-11,2027-07-13\n301,SUITE,1,2027-07-10,2027-07-12\ntotal,2/5\n", occ.body);
            assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n", call(e, "GET", "/reports/occupancy?date=2027-08-01", null).body);

            Res rev = call(e, "GET", "/reports/revenue?month=2027-07", null);
            assertEquals("""
                    reservation,room,check_in,check_out,status,amount
                    3,102,2027-07-05,2027-07-11,CONFIRMED,1799.28
                    1,301,2027-07-10,2027-07-12,CONFIRMED,1391.76
                    2,101,2027-07-11,2027-07-13,CONFIRMED,599.76
                    total,3790.80
                    """, rev.body);
        }
    }

    @Test
    void invalidEmailIsBlocked() throws Exception {
        try (Env e = env(Map.of(), noon())) {
            call(e, "POST", "/reservations", booking(101, "ana@gmail", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            assertEquals(200, call(e, "POST", "/reservations/1/confirmation", null).status);
            JsonNode msgs = call(e, "GET", "/messages", null).json();
            assertEquals(1, msgs.size());
            assertEquals("BLOCKED", msgs.get(0).get("status").asText());
            assertEquals("ana@gmail", msgs.get(0).get("to").asText());
        }
    }

    @Test
    void testModeRedirects() throws Exception {
        try (Env e = env(Map.of("HOTEL_TEST_MODE", "true"), noon())) {
            call(e, "POST", "/reservations", booking(101, "ana@gmail.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            call(e, "POST", "/reservations/1/confirmation", null);
            JsonNode msgs = call(e, "GET", "/messages", null).json();
            assertEquals(1, msgs.size());
            assertEquals("testes@pousadamarealta.com.br", msgs.get(0).get("to").asText());
            assertEquals("[TESTE] Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
            assertTrue(msgs.get(0).get("body").asText().startsWith("Destinatário original: ana@gmail.com\nOlá, "));
        }
    }

    @Test
    void dailyLimitAndQuietHoursFromConfig() throws Exception {
        Map<String, String> limit = Map.of("HOTEL_RULE_DAILY_LIMIT", "ALWAYS", "HOTEL_DAILY_LIMIT", "1",
                "HOTEL_RULE_ARCHIVE_COPY", "NEVER");
        try (Env e = env(limit, noon())) {
            call(e, "POST", "/reservations", booking(101, "ana@gmail.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            call(e, "POST", "/reservations", booking(102, "ANA@gmail.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            call(e, "POST", "/reservations/1/confirmation", null);
            call(e, "POST", "/reservations/2/confirmation", null);
            JsonNode msgs = call(e, "GET", "/messages", null).json();
            assertEquals(2, msgs.size());
            assertEquals("SENT", msgs.get(0).get("status").asText());
            assertEquals("BLOCKED", msgs.get(1).get("status").asText());
        }
        Clock night = Clock.fixed(Instant.parse("2027-03-01T05:00:00Z"), ZoneId.of("UTC")); // 02h em São Paulo
        try (Env e = env(Map.of("HOTEL_RULE_QUIET_HOURS", "LIVE_ONLY"), night)) {
            call(e, "POST", "/reservations", booking(101, "ana@gmail.com", "2027-03-10", "2027-03-12", 2, "STANDARD"));
            call(e, "POST", "/reservations/1/confirmation", null);
            JsonNode msgs = call(e, "GET", "/messages", null).json();
            assertEquals(1, msgs.size());
            assertEquals("BLOCKED", msgs.get(0).get("status").asText());
        }
    }
}
