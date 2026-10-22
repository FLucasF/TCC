package br.com.maralta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
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

/** Teste ponta a ponta; usa um schema descartável no banco de DB_URL (ignorado se o banco não existir). */
class ApiTest {
    static String schema = "teste_" + UUID.randomUUID().toString().replace("-", "");
    static Database database;
    static Javalin app;
    static HttpClient http = HttpClient.newHttpClient();
    static String base;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        Assumptions.assumeTrue(url != null, "DB_URL ausente");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        } catch (Exception e) {
            Assumptions.abort("Banco indisponível: " + e.getMessage());
        }
        database = new Database(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        app = Api.create(new ReservationService(database), new Reports(database)).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
            database.close();
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception {
            return Json.MAPPER.readTree(body);
        }
    }

    static Res call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String body(int room, String ident, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com",%s,
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, ident, in, out,
                guests, plan);
    }

    static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void fluxoCompleto() throws Exception {
        Res created = call("POST", "/reservations", body(101, CPF, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertEquals("599.76", r.get("total").asText());
        assertTrue(r.get("refund").isNull());
        assertTrue(r.get("companyCnpj").isNull());

        assertEquals(409, call("POST", "/reservations", body(101, CPF, "2027-07-11", "2027-07-13", 2, "STANDARD")).status());
        assertEquals(201, call("POST", "/reservations", body(101, CPF, "2027-07-12", "2027-07-13", 2, "STANDARD")).status());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());

        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("599.76", confirmed.json().get("amountPaid").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode msg = null;
        for (JsonNode m : call("GET", "/messages", null).json()) {
            if (m.get("body").asText().contains("Comprovante de reserva nº " + id)) {
                msg = m;
            }
        }
        assertEquals("Reserva confirmada - Pousada Maré Alta", msg.get("subject").asText());
        assertTrue(msg.get("body").asText().startsWith("Olá, Carla Dias!\n\nSua reserva"));
        assertTrue(msg.get("body").asText().contains("Comprovante de reserva nº " + id));

        Res occ = call("GET", "/reports/occupancy?date=2027-07-10", null);
        assertTrue(occ.contentType().contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD," + id
                + ",2027-07-10,2027-07-12\ntotal,1/5\n", occ.body());

        assertEquals(200, call("POST", "/reservations/" + id + "/check-in", null).status());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());
        Res out = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals("Comprovante de reserva", out.json().get("title").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).status());
        assertTrue(call("GET", "/reports/revenue?month=2027-07", null).body().contains("total,599.76\n"));
    }

    @Test
    void cancelamentoNaoReembolsavelMantemReceita() throws Exception {
        long id = call("POST", "/reservations", body(301, "\"guestPassport\":\"X1\",\"guestCountry\":\"UK\"",
                "2027-09-01", "2027-09-02", 4, "NON_REFUNDABLE")).json().get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        Res cancelled = call("POST", "/reservations/" + id + "/cancellation", null);
        assertTrue(cancelled.body().contains("\"refund\":0.00"));
        assertTrue(call("GET", "/reports/revenue?month=2027-09", null).body().contains("CANCELLED,492.92"));
    }

    @Test
    void erros() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nao e json").status());
        assertEquals(400, call("POST", "/reservations", body(101, CPF + ",\"companyCnpj\":\"1\",\"companyName\":\"A\"",
                "2027-01-01", "2027-01-02", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, "\"companyCnpj\":\"1\"",
                "2027-01-01", "2027-01-02", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2027-01-02", "2027-01-02", 1, "STANDARD")).status());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2027-01-01", "2027-01-02", 0, "STANDARD")).status());
        assertEquals(404, call("POST", "/reservations", body(999, CPF, "2027-01-01", "2027-01-02", 1, "STANDARD")).status());
        assertEquals(422, call("POST", "/reservations", body(101, CPF, "2027-01-01", "2027-01-02", 3, "STANDARD")).status());
        assertEquals(404, call("GET", "/reservations/99999", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).status());
        assertEquals(200, call("GET", "/rooms", null).status());
    }
}
