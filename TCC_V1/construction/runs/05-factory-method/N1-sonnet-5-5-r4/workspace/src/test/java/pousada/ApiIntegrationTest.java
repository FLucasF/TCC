package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pousada.persistence.Database;
import pousada.service.ReportService;
import pousada.service.ReservationService;
import pousada.web.Api;

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

/** Roda contra o PostgreSQL de DB_URL, num schema descartável; é ignorado se não houver banco. */
class ApiIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static String schema;
    private static String url;
    private static String user;
    private static String password;
    private static Database db;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        Assumptions.assumeTrue(url != null, "DB_URL ausente");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            Assumptions.abort("banco indisponível: " + e.getMessage());
        }
        db = Database.connect(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        app = new Api(new ReservationService(db), new ReportService(db)).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private record Resp(int status, String body, String contentType) {
        JsonNode json() throws Exception {
            return MAPPER.readTree(body);
        }
    }

    private static Resp call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.method(method, HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
        }
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), r.body(), r.headers().firstValue("Content-Type").orElse(""));
    }

    private static String reservation(int room, String in, String out, int guests, String plan, String identity) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com",%s,
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, identity, in, out, guests, plan);
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void fullFlow() throws Exception {
        Resp created = call("POST", "/reservations", reservation(201, "2031-03-10", "2031-03-12", 2, "STANDARD", CPF));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(779.80, r.get("total").asDouble());
        assertTrue(r.get("refund").isNull());
        assertTrue(r.get("companyCnpj").isNull());
        assertEquals("111.222.333-44", r.get("guestCpf").asText());

        assertEquals(404, call("GET", "/reservations/999999", null).status());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).status());

        Resp conflict = call("POST", "/reservations", reservation(201, "2031-03-11", "2031-03-13", 1, "STANDARD", CPF));
        assertEquals(409, conflict.status());
        assertEquals("ROOM_UNAVAILABLE", conflict.json().get("error").asText());
        // Saída no dia da entrada de outra não conflita.
        assertEquals(201, call("POST", "/reservations", reservation(201, "2031-03-12", "2031-03-13", 1, "STANDARD", CPF)).status());

        Resp confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, confirmed.status());
        assertEquals(779.80, confirmed.json().get("amountPaid").asDouble());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode mine = null;
        int count = 0;
        for (JsonNode m : call("GET", "/messages", null).json()) {
            if (m.get("body").asText().contains("Comprovante de reserva nº " + id + "\n")) {
                mine = m;
                count++;
            }
        }
        assertEquals(1, count);
        assertEquals("Reserva confirmada - Pousada Maré Alta", mine.get("subject").asText());
        assertEquals("EMAIL", mine.get("channel").asText());
        assertEquals("carla@example.com", mine.get("to").asText());
        assertTrue(mine.get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva"));
        assertTrue(mine.get("body").asText().endsWith("A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"));

        Resp doc = call("GET", "/reservations/" + id + "/document", null);
        assertEquals(200, doc.status());
        assertEquals("Comprovante de reserva", doc.json().get("title").asText());
        assertTrue(doc.json().get("guestPassport").isNull());

        Resp occ = call("GET", "/reports/occupancy?date=2031-03-11", null);
        assertTrue(occ.contentType().toLowerCase().startsWith("text/csv"));
        assertTrue(occ.contentType().toLowerCase().contains("utf-8"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2031-03-10,2031-03-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n", call("GET", "/reports/occupancy?date=2031-03-12", null).body());

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());
        Resp out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals(200, out.status());
        assertTrue(out.json().get("text").asText().contains("Total: R$ 779,80"));

        Resp revenue = call("GET", "/reports/revenue?month=2031-03", null);
        assertTrue(revenue.body().startsWith("reservation,room,check_in,check_out,status,amount\n" + id + ",201,2031-03-10,2031-03-12,CHECKED_OUT,779.80\n"));
    }

    @Test
    void nonRefundableCancellationKeepsMoneyAndOtherIdentities() throws Exception {
        String company = "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"";
        JsonNode r = call("POST", "/reservations", reservation(301, "2032-05-01", "2032-05-03", 3, "NON_REFUNDABLE", company)).json();
        long id = r.get("id").asLong();
        assertEquals(985.83, r.get("total").asDouble());
        assertEquals(173.97, r.get("discount").asDouble());
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode cancelled = call("POST", "/reservations/" + id + "/cancellation", null).json();
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(0.0, cancelled.get("refund").asDouble());
        assertTrue(call("GET", "/reports/revenue?month=2032-05", null).body().contains("CANCELLED,985.83\ntotal,985.83\n"));
        // quarto liberado de novo
        String passport = "\"guestPassport\":\"X1\",\"guestCountry\":\"United Kingdom\"";
        Resp again = call("POST", "/reservations", reservation(301, "2032-05-01", "2032-05-03", 3, "STANDARD", passport));
        assertEquals(201, again.status());
        long id2 = again.json().get("id").asLong();
        call("POST", "/reservations/" + id2 + "/confirmation", null);
        Resp doc = call("GET", "/reservations/" + id2 + "/document", null);
        assertEquals("Booking receipt", doc.json().get("title").asText());
        assertTrue(doc.json().get("text").asText().startsWith("Booking receipt no. " + id2));
        JsonNode last = call("GET", "/messages", null).json();
        assertTrue(last.get(last.size() - 1).get("body").asText().contains("Booking receipt no. " + id2));
    }

    @Test
    void validationErrors() throws Exception {
        assertEquals(400, call("POST", "/reservations", "not json").status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2031-03-10", "2031-03-10", 1, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2031-03-10", "2031-03-12", 0, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2031-3-10", "2031-03-12", 1, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2031-03-10", "2031-03-12", 1, "STANDARD", "\"x\":1")).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2031-03-10", "2031-03-12", 1, "STANDARD", CPF + ",\"guestPassport\":\"X\",\"guestCountry\":\"BR\"")).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2031-03-10", "2031-03-12", 1, "STANDARD", "\"companyCnpj\":\"1\"")).status());
        assertEquals(404, call("POST", "/reservations", reservation(999, "2031-03-10", "2031-03-12", 1, "STANDARD", CPF)).status());
        Resp cap = call("POST", "/reservations", reservation(101, "2031-03-10", "2031-03-12", 3, "STANDARD", CPF));
        assertEquals(422, cap.status());
        assertEquals("CAPACITY_EXCEEDED", cap.json().get("error").asText());
        assertEquals(400, call("GET", "/reports/occupancy?date=2031-02-30", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2031-13", null).status());
        assertEquals(400, call("GET", "/reports/revenue", null).status());
        assertEquals(5, call("GET", "/rooms", null).json().size());
    }
}
