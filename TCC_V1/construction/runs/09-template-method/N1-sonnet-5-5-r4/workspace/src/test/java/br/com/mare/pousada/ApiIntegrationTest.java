package br.com.mare.pousada;

import br.com.mare.pousada.infra.Db;
import br.com.mare.pousada.web.Api;
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

/** Roda contra o banco de DB_URL, num schema próprio que some no fim. */
class ApiIntegrationTest {
    static String url, user, password, schema;
    static Db db;
    static Javalin app;
    static String base;
    static final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws Exception {
        url = System.getenv("DB_URL");
        user = System.getenv("DB_USER");
        password = System.getenv("DB_PASSWORD");
        assumeTrue(url != null, "DB_URL não definido");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        db = new Db(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        app = Api.create(db).start(0);
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

    static HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String reservation(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        HttpResponse<String> created = call("POST", "/reservations", reservation(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        String id = created.body().replaceAll(".*\"id\":(\\d+).*", "$1");
        assertTrue(created.body().contains("\"total\":779.80"), created.body());
        assertTrue(created.body().contains("\"refund\":null"), created.body());

        assertEquals(409, call("POST", "/reservations", reservation(201, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", reservation(101, "2027-03-11", "2027-03-13", 3, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", reservation(999, "2027-03-11", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", reservation(101, "2027-03-13", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "nao json").statusCode());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).statusCode());

        HttpResponse<String> confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals(200, confirmed.statusCode());
        assertTrue(confirmed.body().contains("\"amountPaid\":779.80"), confirmed.body());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).statusCode());

        String messages = call("GET", "/messages", null).body();
        assertTrue(messages.contains("Reserva confirmada - Pousada Maré Alta"), messages);
        assertTrue(messages.contains("\"channel\":\"EMAIL\""), messages);

        String occupancy = call("GET", "/reports/occupancy?date=2027-03-11", null).body();
        assertEquals("Pousada Maré Alta,occupancy,2027-03-11\nroom,type,reservation,check_in,check_out\n"
                + "201,DELUXE," + id + ",2027-03-10,2027-03-12\ntotal,1/5\n", occupancy);
        assertTrue(call("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-02-30", null).statusCode());

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).statusCode());
        HttpResponse<String> out = call("POST", "/reservations/" + id + "/check-out", null);
        assertTrue(out.body().contains("\"title\":\"Comprovante de reserva\""), out.body());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).statusCode());

        assertEquals("Pousada Maré Alta,revenue,2027-03\nreservation,room,check_in,check_out,status,amount\n"
                + id + ",201,2027-03-10,2027-03-12,CHECKED_OUT,779.80\ntotal,779.80\n",
                call("GET", "/reports/revenue?month=2027-03", null).body());
        assertEquals("Pousada Maré Alta,tourism-tax,2027-03\nreservation,room,check_in,check_out,guests,nights,tax\n"
                + id + ",201,2027-03-10,2027-03-12,2,2,18.00\ntotal,18.00\n",
                call("GET", "/reports/tourism-tax?month=2027-03", null).body());
        assertEquals("Pousada Maré Alta,rate-plans,2027-03\nrate_plan,reservations,amount\n"
                + "STANDARD,1,779.80\nNON_REFUNDABLE,0,0.00\ntotal,1,779.80\n",
                call("GET", "/reports/rate-plans?month=2027-03", null).body());
    }

    @Test
    void nonRefundableCancelledStillCounts() throws Exception {
        HttpResponse<String> created = call("POST", "/reservations", reservation(301, "2027-07-10", "2027-07-11", 1, "NON_REFUNDABLE"));
        String id = created.body().replaceAll(".*\"id\":(\\d+).*", "$1");
        call("POST", "/reservations/" + id + "/confirmation", null);
        HttpResponse<String> cancelled = call("POST", "/reservations/" + id + "/cancellation", null);
        assertTrue(cancelled.body().contains("\"refund\":0.00"), cancelled.body());
        assertTrue(call("GET", "/reports/revenue?month=2027-07", null).body().contains("CANCELLED,591.50"));
        assertEquals(404, call("GET", "/reservations/99999", null).statusCode());
        assertEquals(200, call("GET", "/rooms", null).statusCode());
    }
}
