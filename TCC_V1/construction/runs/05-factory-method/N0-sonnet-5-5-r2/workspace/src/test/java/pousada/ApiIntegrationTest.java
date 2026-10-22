package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs against the PostgreSQL in DB_URL, inside a throw-away schema. Skipped without DB_URL. */
class ApiIntegrationTest {
    static final ObjectMapper JSON = new ObjectMapper().setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true)).enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String schema;
    static Database db;
    static Javalin app;
    static String base;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null, "DB_URL not set");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        String scoped = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        db = new Database(scoped, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        app = new Api(new ReservationService(db)).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (db != null) db.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception { return JSON.readTree(body); }
    }

    static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String booking(int room, String in, String out, int guests, String plan, String ident) {
        return "{\"roomNumber\":%d,\"guestName\":\"Ana Souza\",\"guestEmail\":\"ana@x.com\",%s,\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"guests\":%d,\"ratePlan\":\"%s\"}"
                .formatted(room, ident, in, out, guests, plan);
    }

    static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void roomsAndErrors() throws Exception {
        Res rooms = call("GET", "/rooms", null);
        assertEquals(5, rooms.json().size());
        assertEquals("249.90", rooms.json().get(0).get("nightlyRate").decimalValue().toPlainString());

        assertEquals("ROOM_NOT_FOUND", call("POST", "/reservations", booking(999, "2030-01-10", "2030-01-12", 1, "STANDARD", CPF)).json().get("error").asText());
        assertEquals(422, call("POST", "/reservations", booking(101, "2030-01-10", "2030-01-12", 3, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2030-01-12", "2030-01-12", 1, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", "not json").status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2030-01-10", "2030-01-12", 1, "STANDARD", CPF + ",\"guestPassport\":\"A\",\"guestCountry\":\"UK\"")).status());
        assertEquals(400, call("POST", "/reservations", booking(101, "2030-01-10", "2030-01-12", 1, "STANDARD", "\"companyCnpj\":\"1\"")).status());
        assertEquals(404, call("GET", "/reservations/99999", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).status());
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-02-30", null).status());
    }

    @Test
    void individualLifecycleWithReportsAndEmail() throws Exception {
        Res created = call("POST", "/reservations", booking(201, "2031-03-10", "2031-03-12", 2, "STANDARD", CPF));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("779.80", r.get("total").decimalValue().toPlainString());
        assertTrue(r.get("refund").isNull());
        assertTrue(r.get("companyCnpj").isNull());

        assertEquals(409, call("POST", "/reservations", booking(201, "2031-03-11", "2031-03-13", 2, "STANDARD", CPF)).status());
        assertEquals(201, call("POST", "/reservations", booking(201, "2031-03-12", "2031-03-13", 2, "STANDARD", CPF)).status());
        assertEquals("OPERATION_NOT_ALLOWED", call("POST", "/reservations/" + id + "/check-in", null).json().get("error").asText());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());

        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("CONFIRMED", confirmed.json().get("status").asText());
        assertEquals("779.80", confirmed.json().get("amountPaid").decimalValue().toPlainString());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode mail = call("GET", "/messages", null).json().get(0);
        assertEquals("EMAIL", mail.get("channel").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", mail.get("subject").asText());
        assertTrue(mail.get("body").asText().startsWith("Olá, Ana Souza!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº " + id));
        assertTrue(mail.get("body").asText().endsWith("Total: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"));

        Res occ = call("GET", "/reports/occupancy?date=2031-03-11", null);
        assertTrue(occ.contentType().toLowerCase().contains("text/csv"));
        assertTrue(occ.contentType().toLowerCase().contains("utf-8"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2031-03-10,2031-03-12\ntotal,1/5\n", occ.body());
        assertTrue(call("GET", "/reports/occupancy?date=2031-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals("CHECKED_IN", call("POST", "/reservations/" + id + "/check-in", null).json().get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());
        JsonNode doc = call("POST", "/reservations/" + id + "/check-out", null).json();
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("111.222.333-44", doc.get("guestCpf").asText());
        assertTrue(doc.get("guestPassport").isNull());
        assertTrue(doc.get("text").asText().contains("Total: R$ 779,80"));
        assertEquals(doc.get("text"), call("GET", "/reservations/" + id + "/document", null).json().get("text"));

        Res rev = call("GET", "/reports/revenue?month=2031-03", null);
        assertTrue(rev.body().startsWith("reservation,room,check_in,check_out,status,amount\n"));
        assertTrue(rev.body().contains(id + ",201,2031-03-10,2031-03-12,CHECKED_OUT,779.80\n"));
    }

    @Test
    void companyAndForeignerDocuments() throws Exception {
        String company = "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"";
        long a = call("POST", "/reservations", booking(202, "2032-03-10", "2032-03-12", 2, "STANDARD", company)).json().get("id").asLong();
        call("POST", "/reservations/" + a + "/confirmation", null);
        JsonNode inv = call("GET", "/reservations/" + a + "/document", null).json();
        assertEquals("Fatura de hospedagem", inv.get("title").asText());
        assertTrue(inv.get("text").asText().endsWith("ISS retido (5%): R$ 38,99"));
        assertTrue(inv.get("guestCpf").isNull());

        String foreign = "\"guestPassport\":\"X1234567\",\"guestCountry\":\"United Kingdom\"";
        long b = call("POST", "/reservations", booking(301, "2032-03-10", "2032-03-12", 2, "NON_REFUNDABLE", foreign)).json().get("id").asLong();
        call("POST", "/reservations/" + b + "/confirmation", null);
        JsonNode receipt = call("GET", "/reservations/" + b + "/document", null).json();
        assertTrue(receipt.get("text").asText().startsWith("Booking receipt no. " + b + "\nGuest: Ana Souza (passport X1234567, United Kingdom)"));

        // e-mail stays Portuguese
        JsonNode mails = call("GET", "/messages", null).json();
        String last = mails.get(mails.size() - 1).get("body").asText();
        assertTrue(last.startsWith("Olá, Ana Souza!") && last.contains("Booking receipt no. " + b) && last.endsWith("Até breve!"));
    }

    @Test
    void cancellationRefunds() throws Exception {
        long pending = call("POST", "/reservations", booking(101, "2033-05-10", "2033-05-12", 2, "STANDARD", CPF)).json().get("id").asLong();
        assertEquals("0.00", call("POST", "/reservations/" + pending + "/cancellation", null).json().get("refund").decimalValue().toPlainString());

        long std = call("POST", "/reservations", booking(101, "2033-05-10", "2033-05-12", 2, "STANDARD", CPF)).json().get("id").asLong();
        call("POST", "/reservations/" + std + "/confirmation", null);
        JsonNode c1 = call("POST", "/reservations/" + std + "/cancellation", null).json();
        assertEquals("CANCELLED", c1.get("status").asText());
        assertEquals(c1.get("amountPaid"), c1.get("refund"));
        assertEquals(409, call("POST", "/reservations/" + std + "/cancellation", null).status());

        long nr = call("POST", "/reservations", booking(102, "2033-05-10", "2033-05-12", 2, "NON_REFUNDABLE", CPF)).json().get("id").asLong();
        call("POST", "/reservations/" + nr + "/confirmation", null);
        JsonNode c2 = call("POST", "/reservations/" + nr + "/cancellation", null).json();
        assertEquals("0.00", c2.get("refund").decimalValue().toPlainString());

        String rev = call("GET", "/reports/revenue?month=2033-05", null).body();
        assertTrue(rev.contains(nr + ",102,2033-05-10,2033-05-12,CANCELLED,424.83\n"), rev);
        assertTrue(!rev.contains("\n" + std + ","), rev);
        assertTrue(rev.endsWith("total,424.83\n"), rev);
    }
}
