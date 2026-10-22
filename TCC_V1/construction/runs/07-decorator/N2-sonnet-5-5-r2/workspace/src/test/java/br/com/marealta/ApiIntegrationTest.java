package br.com.marealta;

import br.com.marealta.messaging.MessageLog;
import br.com.marealta.persistence.*;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Routes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Usa o PostgreSQL de DB_URL/DB_USER/DB_PASSWORD; é pulado se não houver banco. Datas aleatórias evitam colisão entre execuções. */
class ApiIntegrationTest {
    static Javalin app;
    static String base;
    static int year;
    static final HttpClient http = HttpClient.newHttpClient();
    static final ObjectMapper om = new ObjectMapper();

    @BeforeAll
    static void start() {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null, "sem DB_URL");
        DataSource ds;
        try {
            ds = Database.connect(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        } catch (RuntimeException e) {
            assumeTrue(false, "banco indisponível");
            return;
        }
        MessageLog log = new PostgresMessageLog(ds);
        ReservationRepository repo = new ReservationRepository(ds);
        app = new Routes(new ReservationService(repo, Main.messageSender(log, false, true)), repo, log).start(0);
        base = "http://localhost:" + app.port();
        year = 3000 + new Random().nextInt(5000);
    }

    @AfterAll
    static void stop() { if (app != null) app.stop(); }

    record Res(int status, String body) {
        JsonNode json() throws Exception { return om.readTree(body); }
    }

    static Res call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body());
    }

    static String reservation(int room, String in, String out, int guests, String plan, String email) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    @Test
    void fullFlow() throws Exception {
        String in = year + "-07-10", out = year + "-07-12";
        Res created = call("POST", "/reservations", reservation(101, in, out, 2, "STANDARD", "carla@x.com"));
        assertEquals(201, created.status());
        JsonNode r = created.json();
        long id = r.get("id").asLong();
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(599.76, r.get("subtotal").asDouble());
        assertTrue(r.get("refund").isNull());
        assertEquals(409, call("POST", "/reservations", reservation(101, in, year + "-07-11", 1, "STANDARD", "a@b.com")).status());
        assertEquals(409, call("GET", "/reservations/" + id + "/document", null).status());

        Res confirmed = call("POST", "/reservations/" + id + "/confirmation", null);
        assertEquals("CONFIRMED", confirmed.json().get("status").asText());
        assertEquals(599.76, confirmed.json().get("amountPaid").asDouble());
        assertEquals(409, call("POST", "/reservations/" + id + "/confirmation", null).status());

        JsonNode msgs = call("GET", "/messages", null).json();
        int n = msgs.size();
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", msgs.get(n - 1).get("subject").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(n - 2).get("subject").asText());
        assertEquals("SENT", msgs.get(n - 2).get("status").asText());

        Res occ = call("GET", "/reports/occupancy?date=" + in, null);
        assertTrue(occ.body().contains("101,STANDARD," + id + "," + in + "," + out), occ.body());
        assertTrue(occ.body().endsWith("total,1/5\n"), occ.body());
        assertEquals("total,0/5\n", call("GET", "/reports/occupancy?date=" + out, null).body().lines().reduce((a, b) -> b).get() + "\n");

        assertEquals(409, call("POST", "/reservations/" + id + "/check-out", null).status());
        assertEquals("CHECKED_IN", call("POST", "/reservations/" + id + "/check-in", null).json().get("status").asText());
        assertEquals(409, call("POST", "/reservations/" + id + "/cancellation", null).status());
        Res out1 = call("POST", "/reservations/" + id + "/check-out", null);
        assertEquals(200, out1.status());
        assertEquals("Comprovante de reserva", out1.json().get("title").asText());
        assertEquals(200, call("GET", "/reservations/" + id + "/document", null).status());

        Res rev = call("GET", "/reports/revenue?month=" + year + "-07", null);
        assertTrue(rev.body().startsWith("reservation,room,check_in,check_out,status,amount\n"));
        assertTrue(rev.body().contains(id + ",101," + in + "," + out + ",CHECKED_OUT,599.76"), rev.body());
    }

    @Test
    void cancellationAndErrors() throws Exception {
        String in = year + "-03-10", out = year + "-03-12";
        long id = call("POST", "/reservations", reservation(202, in, out, 3, "NON_REFUNDABLE", "ana@gmail")).json().get("id").asLong();
        call("POST", "/reservations/" + id + "/confirmation", null);
        JsonNode msgs = call("GET", "/messages", null).json();
        assertEquals("BLOCKED", msgs.get(msgs.size() - 1).get("status").asText());
        Res cancelled = call("POST", "/reservations/" + id + "/cancellation", null);
        assertEquals(0.0, cancelled.json().get("refund").asDouble());
        assertEquals(662.83, cancelled.json().get("amountPaid").asDouble());
        Res rev = call("GET", "/reports/revenue?month=" + year + "-03", null);
        assertTrue(rev.body().contains(id + ",202," + in + "," + out + ",CANCELLED,662.83"), rev.body());
        // quarto liberado após cancelamento
        assertEquals(201, call("POST", "/reservations", reservation(202, in, out, 1, "STANDARD", "a@b.com")).status());

        assertEquals("INVALID_REQUEST", call("POST", "/reservations", "nope").json().get("error").asText());
        assertEquals(400, call("POST", "/reservations", reservation(101, out, in, 1, "STANDARD", "a@b.com")).status());
        assertEquals(400, call("POST", "/reservations", reservation(101, in, out, 0, "STANDARD", "a@b.com")).status());
        assertEquals(404, call("POST", "/reservations", reservation(999, in, out, 1, "STANDARD", "a@b.com")).status());
        assertEquals(422, call("POST", "/reservations", reservation(101, in, out, 3, "STANDARD", "a@b.com")).status());
        assertEquals(404, call("GET", "/reservations/999999999", null).status());
        assertEquals(400, call("GET", "/reports/occupancy?date=2027-13-01", null).status());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-7", null).status());
        assertEquals(5, call("GET", "/rooms", null).json().size());
    }
}
