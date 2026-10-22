package br.com.marealta;

import br.com.marealta.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ApiTest {
    static final ObjectMapper MAPPER = new ObjectMapper().setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true)).enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    static Javalin app;
    static Database database;
    static String base;
    static String schemaName;
    static String dbUrl;
    static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null, "DB_URL não definido: testes de API ignorados");
        dbUrl = url;
        String schema = schemaName = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        database = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        app = App.create(database).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (database != null) database.close();
        if (schemaName != null) {
            try (Connection c = DriverManager.getConnection(dbUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
                 Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schemaName + " CASCADE");
            }
        }
    }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception { return MAPPER.readTree(body); }
    }

    static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String reservation(int room, String in, String out, int guests, String plan, String identity) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@x.com\"," + identity
                + ",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out + "\",\"guests\":" + guests
                + ",\"ratePlan\":\"" + plan + "\"}";
    }

    static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void fullFlowWithCpf() throws Exception {
        Res rooms = call("GET", "/rooms", null);
        assertEquals(5, rooms.json().size());

        Res created = call("POST", "/reservations", reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD", CPF));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("111.222.333-44", r.get("guestCpf").asText());
        assertTrue(r.get("companyCnpj").isNull());
        assertTrue(r.get("refund").isNull());
        assertEquals("779.80", r.get("total").asText());

        Res conflict = call("POST", "/reservations", reservation(201, "2027-03-11", "2027-03-13", 2, "STANDARD", CPF));
        assertEquals(409, conflict.status());
        assertEquals("ROOM_UNAVAILABLE", conflict.json().get("error").asText());

        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("CONFIRMED", confirmed.json().get("status").asText());
        assertEquals("779.80", confirmed.json().get("amountPaid").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode msg = call("GET", "/messages", null).json().get(0);
        assertEquals("Reserva confirmada - Pousada Maré Alta", msg.get("subject").asText());
        assertTrue(msg.get("body").asText().contains("Comprovante de reserva nº " + id));
        assertTrue(msg.get("body").asText().endsWith("Até breve!"));

        Res occ = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(occ.contentType().toLowerCase().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2027-03-10,2027-03-12\ntotal,1/5\n",
                occ.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals(call("GET", "/reservations/" + id + "/document", null).body(), out.body());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());

        Res rev = call("GET", "/reports/revenue?month=2027-03", null);
        assertTrue(rev.body().contains(id + ",201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\n"));
    }

    @Test
    void nonRefundableCancellationKeepsRevenue() throws Exception {
        String ident = "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"";
        JsonNode r = call("POST", "/reservations", reservation(301, "2027-09-10", "2027-09-11", 4, "NON_REFUNDABLE", ident)).json();
        long id = r.get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode cancelled = call("POST", "/reservations/" + id + "/cancellation", null).json();
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals("0.00", cancelled.get("refund").asText());
        assertTrue(call("GET", "/reports/revenue?month=2027-09", null).body().contains(",CANCELLED,492.92\n"));
        // quarto liberado de novo
        assertEquals(201, call("POST", "/reservations", reservation(301, "2027-09-10", "2027-09-11", 1, "STANDARD", CPF)).status());
        assertTrue(call("GET", "/messages", null).body().contains("ISS retido (5%): R$ 24,65"));
    }

    @Test
    void validation() throws Exception {
        String pass = "\"guestPassport\":\"X1\",\"guestCountry\":\"UK\"";
        assertEquals(400, call("POST", "/reservations", "nao e json").status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-10", 1, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 0, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-3-10", "2027-03-11", 1, "STANDARD", CPF)).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 1, "STANDARD", CPF + "," + pass)).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 1, "STANDARD", "\"guestPassport\":\"X1\"")).status());
        assertEquals(404, call("POST", "/reservations", reservation(999, "2027-03-10", "2027-03-11", 1, "STANDARD", CPF)).status());
        Res cap = call("POST", "/reservations", reservation(101, "2027-03-10", "2027-03-11", 3, "STANDARD", CPF));
        assertEquals(422, cap.status());
        assertEquals("CAPACITY_EXCEEDED", cap.json().get("error").asText());
        assertEquals(404, call("GET", "/reservations/99999", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-13", null).status());
        assertEquals(400, call("GET", "/reports/occupancy", null).status());
        Res ok = call("POST", "/reservations", reservation(102, "2027-03-10", "2027-03-11", 1, "STANDARD", pass));
        assertEquals(201, ok.status());
        assertEquals("UK", ok.json().get("guestCountry").asText());
    }
}
