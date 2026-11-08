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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Teste ponta a ponta; usa um schema descartável no banco de DB_URL e é ignorado se não houver banco. */
class ApiTest {
    static String url, user, password, schema;
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();
    static final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        assumeTrue(url != null, "DB_URL ausente");
        schema = "it_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        Database db = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        db.createSchema();
        app = App.start(db, 0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) app.stop();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception { return mapper.readTree(body); }
    }

    static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("Content-Type").orElse(""));
    }

    static String booking(int room, String in, String out, int guests, String plan, String extra) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@x.com\","
                + "\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out
                + "\",\"guests\":" + guests + ",\"ratePlan\":\"" + plan + "\"" + extra + "}";
    }

    @Test
    void fullFlow() throws Exception {
        assertEquals(5, call("GET", "/rooms", null).json().size());

        Res created = call("POST", "/reservations",
                booking(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", ",\"companyCode\":\"TECNOSUL\""));
        assertEquals(201, created.status());
        long id = created.json().get("id").asLong();
        assertEquals("686.22", created.json().get("total").asText());
        assertEquals("PENDING", created.json().get("status").asText());
        assertTrue(created.json().get("refund").isNull());

        assertEquals(409, call("POST", "/reservations",
                booking(201, "2027-03-11", "2027-03-13", 2, "STANDARD", "")).status());
        assertEquals(422, call("POST", "/reservations",
                booking(101, "2027-03-11", "2027-03-13", 3, "STANDARD", "")).status());
        assertEquals(400, call("POST", "/reservations",
                booking(101, "2027-03-11", "2027-03-13", 2, "CORPORATE", "")).status());
        assertEquals(404, call("POST", "/reservations",
                booking(999, "2027-03-11", "2027-03-13", 2, "STANDARD", "")).status());
        assertEquals(400, call("POST", "/reservations", "nao e json").status());

        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/check-in", null).status());
        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("686.22", confirmed.json().get("amountPaid").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode messages = call("GET", "/messages", null).json();
        assertEquals(1, messages.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
        assertTrue(messages.get(0).get("body").asText().contains("Desconto: R$ 93,58\nTotal: R$ 686,22"));

        Res occ = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(occ.contentType().toLowerCase().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE," + id + ",2027-03-10,2027-03-12\ntotal,1/5\n",
                occ.body());
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).status());

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals("686.22", out.json().get("total").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());

        // pacote de fim de semana cancelado depois de pago devolve metade; não reembolsável não devolve
        long pkg = call("POST", "/reservations", booking(202, "2027-03-12", "2027-03-14", 2, "WEEKEND_PACKAGE", ""))
                .json().get("id").asLong();
        call("POST", "/reservations/" + pkg + "/confirmation", null);
        Res cancelled = call("POST", "/reservations/" + pkg + "/cancellation", null);
        assertTrue(cancelled.body().contains("\"refund\":410.00"));
        long nr = call("POST", "/reservations", booking(202, "2027-03-12", "2027-03-14", 2, "NON_REFUNDABLE", ""))
                .json().get("id").asLong();
        call("POST", "/reservations/" + nr + "/confirmation", null);
        call("POST", "/reservations/" + nr + "/cancellation", null);

        Res revenue = call("GET", "/reports/revenue?month=2027-03", null);
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + id + ",201,2027-03-10,2027-03-12,CHECKED_OUT,686.22\n"
                + pkg + ",202,2027-03-12,2027-03-14,CANCELLED,410.00\n"
                + nr + ",202,2027-03-12,2027-03-14,CANCELLED,662.83\n"
                + "total,1759.05\n", revenue.body());
        assertEquals(404, call("GET", "/reservations/9999", null).status());
    }
}
