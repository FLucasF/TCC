package br.com.pousadamarealta.reservas;

import br.com.pousadamarealta.reservas.persistence.Database;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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

/** Teste ponta a ponta contra o PostgreSQL de DB_URL, num esquema temporário. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiTest {
    static final ObjectMapper JSON = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String schema;
    static Database database;
    static Javalin server;
    static String base;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null && !url.isBlank(), "DB_URL não definido");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
        }
        String scoped = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        database = new Database(scoped, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        server = Main.start(database, 0);
        base = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (server != null) server.stop();
        if (database != null) database.close();
        if (schema != null) {
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement st = c.createStatement()) {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    record Res(int status, String body, String contentType) {
        JsonNode json() throws Exception {
            return JSON.readTree(body);
        }
    }

    static Res call(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var r = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Res(r.statusCode(), r.body(), r.headers().firstValue("content-type").orElse(""));
    }

    static String booking(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    static void assertError(Res r, int status, String code) throws Exception {
        assertEquals(status, r.status(), r.body());
        assertEquals(code, r.json().get("error").asText());
    }

    @Test
    @Order(1)
    void listsRooms() throws Exception {
        Res r = call("GET", "/rooms", null);
        assertEquals(200, r.status());
        JsonNode rooms = r.json();
        assertEquals(5, rooms.size());
        assertEquals(101, rooms.get(0).get("number").asInt());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
        assertEquals(4, rooms.get(4).get("capacity").asInt());
        assertEquals(579.90, rooms.get(4).get("nightlyRate").asDouble());
        assertTrue(r.body().contains("\"nightlyRate\":249.90"), r.body());
    }

    @Test
    @Order(2)
    void createsReservationAndValidates() throws Exception {
        Res r = call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.status(), r.body());
        JsonNode j = r.json();
        assertEquals(1, j.get("id").asInt());
        assertEquals("PENDING", j.get("status").asText());
        assertEquals(2, j.get("nights").asInt());
        assertTrue(r.body().contains("\"subtotal\":599.76"), r.body());
        assertTrue(r.body().contains("\"discount\":0.00"), r.body());
        assertTrue(r.body().contains("\"amountPaid\":0.00"), r.body());
        assertTrue(j.get("refund").isNull());
        assertEquals("2027-07-10", j.get("checkIn").asText());

        assertEquals(j, call("GET", "/reservations/1", null).json());

        assertError(call("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "[]"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-10", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "10/07/2027", "2027-07-12", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(101, "2027-07-10", "2027-07-12", 1, "OUTRO")), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", "{\"roomNumber\":101}"), 400, "INVALID_REQUEST");
        assertError(call("POST", "/reservations", booking(999, "2027-07-10", "2027-07-12", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(call("POST", "/reservations", booking(101, "2027-09-10", "2027-09-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(call("POST", "/reservations", booking(101, "2027-07-11", "2027-07-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        assertEquals(201, call("POST", "/reservations", booking(101, "2027-07-12", "2027-07-14", 1, "STANDARD")).status());
        assertError(call("GET", "/reservations/999", null), 404, "RESERVATION_NOT_FOUND");
        assertError(call("GET", "/reservations/abc", null), 404, "RESERVATION_NOT_FOUND");
    }

    @Test
    @Order(3)
    void confirmationNotifiesEveryone() throws Exception {
        Res r = call("POST", "/reservations/1/confirmation", null);
        assertEquals(200, r.status(), r.body());
        assertEquals("CONFIRMED", r.json().get("status").asText());
        assertTrue(r.body().contains("\"amountPaid\":599.76"), r.body());
        assertError(call("POST", "/reservations/1/confirmation", null), 409, "OPERATION_NOT_ALLOWED");

        JsonNode messages = call("GET", "/messages", null).json();
        assertEquals(3, messages.size());
        assertEquals("EMAIL", messages.get(0).get("channel").asText());
        assertEquals("carla@example.com", messages.get(0).get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
        assertEquals("Reserva confirmada - quarto 101", messages.get(1).get("subject").asText());
        assertEquals("Pagamento recebido - reserva 1", messages.get(2).get("subject").asText());
        assertTrue(messages.get(2).get("body").asText().contains("R$ 599,76"));
    }

    @Test
    @Order(4)
    void documentAndLifecycle() throws Exception {
        Res doc = call("GET", "/reservations/1/document", null);
        assertEquals(200, doc.status());
        assertEquals("Comprovante de reserva", doc.json().get("title").asText());
        assertEquals("STANDARD", doc.json().get("roomType").asText());
        assertEquals(1, doc.json().get("reservationId").asInt());
        assertError(call("GET", "/reservations/2/document", null), 409, "OPERATION_NOT_ALLOWED");

        assertError(call("POST", "/reservations/1/check-out", null), 409, "OPERATION_NOT_ALLOWED");
        assertEquals("CHECKED_IN", call("POST", "/reservations/1/check-in", null).json().get("status").asText());
        assertError(call("POST", "/reservations/1/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        Res out = call("POST", "/reservations/1/check-out", null);
        assertEquals(200, out.status(), out.body());
        assertEquals(doc.json(), out.json());
        assertEquals(200, call("GET", "/reservations/1/document", null).status());
    }

    @Test
    @Order(5)
    void cancellations() throws Exception {
        int before = call("GET", "/messages", null).json().size();

        // pendente: cancela sem avisar ninguém
        assertEquals(201, call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD")).status()); // 3
        Res c = call("POST", "/reservations/3/cancellation", null);
        assertEquals("CANCELLED", c.json().get("status").asText());
        assertTrue(c.body().contains("\"refund\":0.00"), c.body());
        assertEquals(before, call("GET", "/messages", null).json().size());

        // a noite foi liberada
        assertEquals(201, call("POST", "/reservations", booking(201, "2027-03-10", "2027-03-12", 2, "STANDARD")).status()); // 4
        call("POST", "/reservations/4/confirmation", null);
        Res refunded = call("POST", "/reservations/4/cancellation", null);
        assertTrue(refunded.body().contains("\"refund\":779.80"), refunded.body());
        assertError(call("POST", "/reservations/4/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode m = call("GET", "/messages", null).json();
        assertEquals(before + 3 + 2, m.size()); // confirmação (3) + cancelamento (governança e financeiro)
        assertEquals("Reserva cancelada - quarto 201", m.get(m.size() - 2).get("subject").asText());
        assertEquals("Reembolso - reserva 4", m.get(m.size() - 1).get("subject").asText());

        // não reembolsável: só a governança é avisada
        assertEquals(201, call("POST", "/reservations", booking(202, "2027-03-10", "2027-03-11", 1, "NON_REFUNDABLE")).status()); // 5
        call("POST", "/reservations/5/confirmation", null);
        int mid = call("GET", "/messages", null).json().size();
        Res nr = call("POST", "/reservations/5/cancellation", null);
        assertTrue(nr.body().contains("\"refund\":0.00"), nr.body());
        assertEquals(mid + 1, call("GET", "/messages", null).json().size());
    }

    @Test
    @Order(6)
    void reports() throws Exception {
        // reserva 2: 101, 12 a 14/07, pendente; criar paga em 301 e 102
        call("POST", "/reservations/2/confirmation", null);
        assertEquals(201, call("POST", "/reservations", booking(301, "2027-07-10", "2027-07-11", 4, "NON_REFUNDABLE")).status()); // 6
        call("POST", "/reservations/6/confirmation", null);

        Res occ = call("GET", "/reports/occupancy?date=2027-07-12", null);
        assertEquals(200, occ.status());
        assertEquals("text/csv;charset=utf-8", occ.contentType().replace(" ", ""));
        assertEquals("""
                room,type,reservation,check_in,check_out
                101,STANDARD,2,2027-07-12,2027-07-14
                total,1/5
                """, occ.body());
        assertEquals("""
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-12
                301,SUITE,6,2027-07-10,2027-07-11
                total,2/5
                """, call("GET", "/reports/occupancy?date=2027-07-10", null).body());
        assertError(call("GET", "/reports/occupancy?date=10-07-2027", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/occupancy", null), 400, "INVALID_REQUEST");

        Res rev = call("GET", "/reports/revenue?month=2027-07", null);
        assertEquals(200, rev.status());
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
                6,301,2027-07-10,2027-07-11,CONFIRMED,591.50
                2,101,2027-07-12,2027-07-14,CONFIRMED,599.76
                total,1791.02
                """, rev.body());
        // março: 5 (não reembolsável cancelada) conta; 3 e 4 foram devolvidas por inteiro
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                5,202,2027-03-10,2027-03-11,CANCELLED,331.42
                total,331.42
                """, call("GET", "/reports/revenue?month=2027-03", null).body());
        assertError(call("GET", "/reports/revenue?month=2027-13", null), 400, "INVALID_REQUEST");
        assertError(call("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
    }
}
