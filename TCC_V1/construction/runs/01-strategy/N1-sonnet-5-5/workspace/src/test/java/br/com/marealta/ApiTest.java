package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.com.marealta.infra.Db;
import br.com.marealta.infra.MessageRepository;
import br.com.marealta.infra.ReservationRepository;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.web.Api;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Roda contra o PostgreSQL de DB_URL, mas num schema próprio que é apagado no fim. */
class ApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String schema;
    private static Db db;
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null, "DB_URL não definida");
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
             Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
        }
        db = new Db(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        ReservationRepository reservations = new ReservationRepository();
        app = new Api(new ReservationService(db, reservations, new MessageRepository()),
                new ReportService(db, reservations)).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterAll
    static void stop() throws Exception {
        if (app != null) {
            app.stop();
            db.close();
            try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD")); Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private static HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path));
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String booking(int room, String in, String out, int guests, String plan, String extra) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@example.com\","
                + "\"guestCpf\":\"111.222.333-44\",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out + "\","
                + "\"guests\":" + guests + ",\"ratePlan\":\"" + plan + "\"" + extra + "}";
    }

    private static JsonNode create(String body) throws Exception {
        HttpResponse<String> response = send("POST", "/reservations", body);
        assertEquals(201, response.statusCode(), response.body());
        return MAPPER.readTree(response.body());
    }

    private static void assertError(int status, String code, HttpResponse<String> response) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        assertEquals(code, MAPPER.readTree(response.body()).get("error").asText());
    }

    @Test
    void listsRooms() throws Exception {
        JsonNode rooms = MAPPER.readTree(send("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(249.90, rooms.get(0).get("nightlyRate").asDouble());
    }

    @Test
    void fullLifecycleWithConfirmationEmailAndReceipt() throws Exception {
        JsonNode created = create(booking(201, "2030-03-11", "2030-03-13", 2, "CORPORATE",
                ",\"companyCode\":\"TECNOSUL\""));
        long id = created.get("id").asLong();
        String raw = send("GET", "/reservations/" + id, null).body();
        assertTrue(raw.contains("\"subtotal\":779.80") && raw.contains("\"amountPaid\":0.00")
                && raw.contains("\"refund\":null"), raw);
        assertEquals("PENDING", created.get("status").asText());
        assertEquals("779.80", created.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("93.58", created.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("686.22", created.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals("0.00", created.get("amountPaid").decimalValue().setScale(2).toPlainString());
        assertTrue(created.get("refund").isNull());

        assertError(409, "OPERATION_NOT_ALLOWED", send("GET", "/reservations/" + id + "/document", null));
        assertError(409, "OPERATION_NOT_ALLOWED", send("POST", "/reservations/" + id + "/check-in", null));

        JsonNode confirmed = MAPPER.readTree(send("POST", "/reservations/" + id + "/confirmation", null).body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("686.22", confirmed.get("amountPaid").decimalValue().setScale(2).toPlainString());

        JsonNode messages = MAPPER.readTree(send("GET", "/messages", null).body());
        JsonNode message = messages.get(messages.size() - 1);
        assertEquals("EMAIL", message.get("channel").asText());
        assertEquals("carla@example.com", message.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", message.get("subject").asText());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. "
                + "Estes são os dados dela:\n\n"
                + "Comprovante de reserva nº " + id + "\n"
                + "Hóspede: Carla Dias (CPF 111.222.333-44)\n"
                + "Quarto: 201 (DELUXE)\n"
                + "Entrada: 11/03/2030 | Saída: 13/03/2030 | 2 diárias\n"
                + "Diárias: R$ 779,80\n"
                + "Desconto: R$ 93,58\n"
                + "Total: R$ 686,22\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", message.get("body").asText());

        assertError(409, "OPERATION_NOT_ALLOWED", send("POST", "/reservations/" + id + "/confirmation", null));
        assertEquals("CHECKED_IN",
                MAPPER.readTree(send("POST", "/reservations/" + id + "/check-in", null).body()).get("status").asText());
        assertError(409, "OPERATION_NOT_ALLOWED", send("POST", "/reservations/" + id + "/cancellation", null));

        JsonNode receipt = MAPPER.readTree(send("POST", "/reservations/" + id + "/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals("686.22", receipt.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals(receipt, MAPPER.readTree(send("GET", "/reservations/" + id + "/document", null).body()));
    }

    @Test
    void roomCannotBeBookedTwiceButCancelledFreesIt() throws Exception {
        String body = booking(101, "2030-05-10", "2030-05-12", 2, "STANDARD", "");
        long first = create(body).get("id").asLong();
        assertError(409, "ROOM_UNAVAILABLE", send("POST", "/reservations",
                booking(101, "2030-05-11", "2030-05-13", 1, "STANDARD", "")));
        create(booking(101, "2030-05-12", "2030-05-14", 1, "STANDARD", ""));
        send("POST", "/reservations/" + first + "/cancellation", null);
        create(body);
    }

    @Test
    void cancellationRefundsFollowThePlan() throws Exception {
        long weekend = create(booking(201, "2030-03-15", "2030-03-17", 2, "WEEKEND_PACKAGE", "")).get("id").asLong();
        send("POST", "/reservations/" + weekend + "/confirmation", null);
        JsonNode cancelled = MAPPER.readTree(send("POST", "/reservations/" + weekend + "/cancellation", null).body());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals("410.00", cancelled.get("refund").decimalValue().setScale(2).toPlainString());

        long nonRefundable = create(booking(202, "2030-03-15", "2030-03-17", 2, "NON_REFUNDABLE", "")).get("id")
                .asLong();
        send("POST", "/reservations/" + nonRefundable + "/confirmation", null);
        assertEquals("0.00", MAPPER.readTree(send("POST", "/reservations/" + nonRefundable + "/cancellation", null)
                .body()).get("refund").decimalValue().setScale(2).toPlainString());

        long pending = create(booking(301, "2030-03-15", "2030-03-17", 2, "STANDARD", "")).get("id").asLong();
        assertEquals("0.00", MAPPER.readTree(send("POST", "/reservations/" + pending + "/cancellation", null)
                .body()).get("refund").decimalValue().setScale(2).toPlainString());
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        assertError(400, "INVALID_REQUEST", send("POST", "/reservations", "não é json"));
        assertError(400, "INVALID_REQUEST", send("POST", "/reservations", "{}"));
        assertError(400, "INVALID_REQUEST",
                send("POST", "/reservations", booking(101, "2032-05-12", "2032-05-12", 1, "STANDARD", "")));
        assertError(400, "INVALID_REQUEST",
                send("POST", "/reservations", booking(101, "2032-05-10", "2032-05-12", 0, "STANDARD", "")));
        assertError(400, "INVALID_REQUEST",
                send("POST", "/reservations", booking(101, "2032-05-10", "2032-05-12", 1, "CORPORATE", "")));
        assertError(400, "INVALID_REQUEST",
                send("POST", "/reservations", booking(101, "2032-05-10", "2032-05-12", 1, "LONG_STAY", "")));
        assertError(400, "INVALID_REQUEST",
                send("POST", "/reservations", booking(101, "2032-05-10", "2032-05-12", 1, "WEEKEND_PACKAGE", "")));
        assertError(404, "ROOM_NOT_FOUND",
                send("POST", "/reservations", booking(999, "2032-05-10", "2032-05-12", 1, "STANDARD", "")));
        assertError(422, "CAPACITY_EXCEEDED",
                send("POST", "/reservations", booking(101, "2032-05-10", "2032-05-12", 3, "STANDARD", "")));
        assertError(404, "RESERVATION_NOT_FOUND", send("GET", "/reservations/99999", null));
        assertError(404, "RESERVATION_NOT_FOUND", send("POST", "/reservations/abc/confirmation", null));
        assertError(400, "INVALID_REQUEST", send("GET", "/reports/occupancy?date=10-07-2027", null));
        assertError(400, "INVALID_REQUEST", send("GET", "/reports/revenue?month=2027-13", null));
        assertError(400, "INVALID_REQUEST", send("GET", "/reports/revenue", null));
    }

    @Test
    void reports() throws Exception {
        long paid = create(booking(102, "2031-07-10", "2031-07-12", 2, "STANDARD", "")).get("id").asLong();
        send("POST", "/reservations/" + paid + "/confirmation", null);
        long refunded = create(booking(101, "2031-07-11", "2031-07-13", 2, "NON_REFUNDABLE", "")).get("id").asLong();
        send("POST", "/reservations/" + refunded + "/confirmation", null);
        send("POST", "/reservations/" + refunded + "/cancellation", null);
        long pending = create(booking(201, "2031-07-11", "2031-07-13", 2, "STANDARD", "")).get("id").asLong();

        HttpResponse<String> occupancy = send("GET", "/reports/occupancy?date=2031-07-11", null);
        assertEquals("text/csv; charset=utf-8", occupancy.headers().firstValue("Content-Type").orElse(""));
        assertEquals("room,type,reservation,check_in,check_out\n102,STANDARD," + paid + ",2031-07-10,2031-07-12\n"
                + "total,1/5\n", occupancy.body());
        assertTrue(send("GET", "/reports/occupancy?date=2031-07-12", null).body().endsWith("total,0/5\n"));

        String revenue = send("GET", "/reports/revenue?month=2031-07", null).body();
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + paid + ",102,2031-07-10,2031-07-12,CONFIRMED,599.76\n"
                + refunded + ",101,2031-07-11,2031-07-13,CANCELLED,509.80\n"
                + "total,1109.56\n", revenue.replace(pending + ",", pending + ","));
    }
}
