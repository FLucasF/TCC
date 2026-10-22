package br.com.marealta;

import br.com.marealta.persistence.Database;
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

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient http = HttpClient.newHttpClient();
    private Database db;
    private Javalin app;
    private String base;

    @BeforeAll
    void start() {
        Assumptions.assumeTrue(System.getenv("DB_URL") != null, "DB_URL não definido");
        db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        app = Application.start(db, 0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    void stop() {
        if (app != null) app.stop();
        if (db != null) db.close();
    }

    @BeforeEach
    void clean() throws Exception {
        try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")); Statement s = c.createStatement()) {
            s.execute("TRUNCATE reservations, messages RESTART IDENTITY");
        }
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return mapper.readTree(r.body());
    }

    private static String body(int room, String ident, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com",%s,
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, ident, in, out, guests, plan);
    }

    private static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void roomsAndCreation() throws Exception {
        assertEquals(5, json(send("GET", "/rooms", null)).size());
        HttpResponse<String> r = send("POST", "/reservations", body(101, CPF, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        JsonNode j = json(r);
        assertEquals(1, j.get("id").asInt());
        assertEquals("599.76", j.get("total").asText());
        assertEquals("PENDING", j.get("status").asText());
        assertTrue(r.body().contains("\"amountPaid\":0.00"), r.body());
        assertTrue(j.get("refund").isNull());
        assertTrue(j.get("companyCnpj").isNull());
        assertTrue(r.body().contains("\"total\":599.76"));
        assertEquals(j, json(send("GET", "/reservations/1", null)));
    }

    @Test
    void validationErrors() throws Exception {
        String[] bad = {
                "not json",
                body(101, CPF, "2027-07-12", "2027-07-12", 2, "STANDARD"),
                body(101, CPF, "2027-07-10", "2027-07-12", 0, "STANDARD"),
                body(101, CPF, "2027-7-10", "2027-07-12", 2, "STANDARD"),
                body(101, "\"guestPassport\":\"X\"", "2027-07-10", "2027-07-12", 2, "STANDARD"),
                body(101, CPF + ",\"guestPassport\":\"X\",\"guestCountry\":\"UK\"", "2027-07-10", "2027-07-12", 2, "STANDARD"),
                body(101, "\"x\":1", "2027-07-10", "2027-07-12", 2, "STANDARD"),
        };
        for (String b : bad) {
            HttpResponse<String> r = send("POST", "/reservations", b);
            assertEquals(400, r.statusCode(), b);
            assertEquals("INVALID_REQUEST", json(r).get("error").asText());
        }
        assertEquals(404, send("POST", "/reservations", body(999, CPF, "2027-07-10", "2027-07-12", 2, "STANDARD")).statusCode());
        assertEquals(422, send("POST", "/reservations", body(101, CPF, "2027-07-10", "2027-07-12", 3, "STANDARD")).statusCode());
        assertEquals(404, send("GET", "/reservations/55", null).statusCode());
        assertEquals("RESERVATION_NOT_FOUND", json(send("GET", "/reservations/abc", null)).get("error").asText());
    }

    @Test
    void overlapAndCancelFreesRoom() throws Exception {
        send("POST", "/reservations", body(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(409, send("POST", "/reservations", body(101, CPF, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(201, send("POST", "/reservations", body(101, CPF, "2027-03-12", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(200, send("POST", "/reservations/1/cancellation", null).statusCode());
        assertEquals(201, send("POST", "/reservations", body(101, CPF, "2027-03-11", "2027-03-12", 2, "STANDARD")).statusCode());
    }

    @Test
    void fullFlowWithEmailDocumentsAndReports() throws Exception {
        String company = "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"";
        send("POST", "/reservations", body(201, company, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(409, send("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, send("GET", "/reservations/1/document", null).statusCode());

        HttpResponse<String> confirmedResponse = send("POST", "/reservations/1/confirmation", null);
        JsonNode confirmed = json(confirmedResponse);
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertTrue(confirmedResponse.body().contains("\"amountPaid\":779.80"), confirmedResponse.body());
        JsonNode msgs = json(send("GET", "/messages", null));
        assertEquals(1, msgs.size());
        assertEquals("EMAIL", msgs.get(0).get("channel").asText());
        assertEquals("carla@x.com", msgs.get(0).get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertTrue(msgs.get(0).get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta"));
        assertTrue(msgs.get(0).get("body").asText().contains("ISS retido (5%): R$ 38,99\n\nA entrada"));

        HttpResponse<String> occ = send("GET", "/reports/occupancy?date=2027-03-10", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("content-type").orElse(""));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                send("GET", "/reports/occupancy?date=2027-03-12", null).body());

        send("POST", "/reservations/1/check-in", null);
        HttpResponse<String> out = send("POST", "/reservations/1/check-out", null);
        assertEquals(200, out.statusCode());
        JsonNode doc = json(out);
        assertEquals("Fatura de hospedagem", doc.get("title").asText());
        assertEquals("Tecnosul Ltda", doc.get("companyName").asText());
        assertTrue(doc.get("guestCpf").isNull());
        assertEquals(doc, json(send("GET", "/reservations/1/document", null)));

        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                send("GET", "/reports/revenue?month=2027-03", null).body());
        assertEquals(400, send("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, send("GET", "/reports/occupancy", null).statusCode());
    }

    @Test
    void nonRefundableCancellationStaysInRevenue() throws Exception {
        String passport = "\"guestPassport\":\"X1234567\",\"guestCountry\":\"United Kingdom\"";
        send("POST", "/reservations", body(301, passport, "2027-03-10", "2027-03-11", 4, "NON_REFUNDABLE"));
        send("POST", "/reservations/1/confirmation", null);
        HttpResponse<String> cancelled = send("POST", "/reservations/1/cancellation", null);
        JsonNode c = json(cancelled);
        assertEquals("CANCELLED", c.get("status").asText());
        assertTrue(cancelled.body().contains("\"refund\":0.00"), cancelled.body());
        assertTrue(json(send("GET", "/messages", null)).get(0).get("body").asText().contains("Booking receipt no. 1"));
        assertEquals("reservation,room,check_in,check_out,status,amount\n1,301,2027-03-10,2027-03-11,CANCELLED,492.92\ntotal,492.92\n",
                send("GET", "/reports/revenue?month=2027-03", null).body());
    }
}
