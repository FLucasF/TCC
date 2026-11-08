package br.com.marealta;

import br.com.marealta.app.ReservationService;
import br.com.marealta.domain.RoomCatalog;
import br.com.marealta.notification.*;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.web.WebApp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SystemTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private static final String RECEIPT_7 = """
            Comprovante de reserva nº 1
            Hóspede: Carla Dias (CPF 111.222.333-44)
            Quarto: 201 (DELUXE)
            Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
            Diárias: R$ 779,80
            Total: R$ 779,80""";

    private Javalin app;
    private String base;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() {
        var rooms = new RoomCatalog();
        var reservations = new InMemoryRepositories.Reservations();
        var messages = new InMemoryRepositories.Messages();
        var notifier = new Notifier(List.of(new GuestSubscriber(rooms), new HousekeepingSubscriber(),
                new FinanceSubscriber()), messages);
        var service = new ReservationService(rooms, reservations, notifier);
        app = new WebApp(rooms, service, messages, new OccupancyReport(rooms, reservations),
                new RevenueReport(reservations)).create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path));
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String body(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@example.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    private JsonNode create(int room, String in, String out, int guests, String plan) throws Exception {
        var r = send("POST", "/reservations", body(room, in, out, guests, plan));
        assertEquals(201, r.statusCode(), r.body());
        assertTrue(r.body().contains("\"amountPaid\":0.00"), r.body());
        return JSON.readTree(r.body());
    }

    private void assertError(HttpResponse<String> r, int status, String code) throws Exception {
        assertEquals(status, r.statusCode(), r.body());
        assertEquals(code, JSON.readTree(r.body()).get("error").asText());
    }

    private JsonNode messages() throws Exception {
        return JSON.readTree(send("GET", "/messages", null).body());
    }

    @Test
    void roomsAreListed() throws Exception {
        JsonNode rooms = JSON.readTree(send("GET", "/rooms", null).body());
        assertEquals(5, rooms.size());
        assertEquals(301, rooms.get(4).get("number").asInt());
        assertEquals("SUITE", rooms.get(4).get("type").asText());
        assertEquals(4, rooms.get(4).get("capacity").asInt());
        assertEquals(579.90, rooms.get(4).get("nightlyRate").asDouble());
    }

    @Test
    void createsPendingReservation() throws Exception {
        JsonNode r = create(101, "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE");
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals("599.76", r.get("subtotal").decimalValue().setScale(2).toPlainString());
        assertEquals("89.96", r.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals("509.80", r.get("total").decimalValue().setScale(2).toPlainString());
        assertEquals("0.00", r.get("amountPaid").decimalValue().setScale(2).toPlainString());
        assertTrue(r.get("refund").isNull());
        assertEquals("2027-07-10", r.get("checkIn").asText());
    }

    @Test
    void confirmationSendsGuestHousekeepingAndFinanceMessages() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        var r = send("POST", "/reservations/1/confirmation", null);
        assertEquals(200, r.statusCode());
        assertEquals("CONFIRMED", JSON.readTree(r.body()).get("status").asText());
        assertEquals("779.80", JSON.readTree(r.body()).get("amountPaid").decimalValue().setScale(2).toPlainString());

        JsonNode msgs = messages();
        assertEquals(3, msgs.size());
        JsonNode guest = null, house = null, fin = null;
        for (JsonNode m : msgs) {
            assertEquals("EMAIL", m.get("channel").asText());
            switch (m.get("to").asText()) {
                case "carla@example.com" -> guest = m;
                case "governanca@pousadamarealta.com.br" -> house = m;
                case "financeiro@pousadamarealta.com.br" -> fin = m;
                default -> fail(m.toString());
            }
        }
        assertEquals("Reserva confirmada - Pousada Maré Alta", guest.get("subject").asText());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. "
                + "Estes são os dados dela:\n\n" + RECEIPT_7 + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", guest.get("body").asText());
        assertEquals("Reserva confirmada - quarto 201", house.get("subject").asText());
        assertTrue(house.get("body").asText().contains("10/03/2027"));
        assertTrue(house.get("body").asText().contains("12/03/2027"));
        assertEquals("Pagamento recebido - reserva 1", fin.get("subject").asText());
        assertTrue(fin.get("body").asText().contains("R$ 779,80"));
    }

    @Test
    void cancellingConfirmedStandardNotifiesAndRefunds() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        send("POST", "/reservations/1/confirmation", null);
        JsonNode r = JSON.readTree(send("POST", "/reservations/1/cancellation", null).body());
        assertEquals("CANCELLED", r.get("status").asText());
        assertEquals("779.80", r.get("refund").decimalValue().setScale(2).toPlainString());
        JsonNode msgs = messages();
        assertEquals(5, msgs.size());
        var subjects = List.of(msgs.get(3).get("subject").asText(), msgs.get(4).get("subject").asText());
        assertTrue(subjects.contains("Reserva cancelada - quarto 201"));
        assertTrue(subjects.contains("Reembolso - reserva 1"));
    }

    @Test
    void cancellingNonRefundableDoesNotNotifyFinance() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE");
        send("POST", "/reservations/1/confirmation", null);
        JsonNode r = JSON.readTree(send("POST", "/reservations/1/cancellation", null).body());
        assertEquals("0.00", r.get("refund").decimalValue().setScale(2).toPlainString());
        JsonNode msgs = messages();
        assertEquals(4, msgs.size());
        assertEquals("Reserva cancelada - quarto 201", msgs.get(3).get("subject").asText());
    }

    @Test
    void cancellingPendingNotifiesNobody() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        JsonNode r = JSON.readTree(send("POST", "/reservations/1/cancellation", null).body());
        assertEquals("0.00", r.get("refund").decimalValue().setScale(2).toPlainString());
        assertEquals(0, messages().size());
    }

    @Test
    void lifecycleAndDocument() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(send("GET", "/reservations/1/document", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(send("POST", "/reservations/1/check-in", null), 409, "OPERATION_NOT_ALLOWED");
        send("POST", "/reservations/1/confirmation", null);
        assertError(send("POST", "/reservations/1/confirmation", null), 409, "OPERATION_NOT_ALLOWED");
        assertError(send("POST", "/reservations/1/check-out", null), 409, "OPERATION_NOT_ALLOWED");
        assertEquals("CHECKED_IN", JSON.readTree(send("POST", "/reservations/1/check-in", null).body())
                .get("status").asText());
        assertError(send("POST", "/reservations/1/cancellation", null), 409, "OPERATION_NOT_ALLOWED");
        JsonNode receipt = JSON.readTree(send("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", receipt.get("title").asText());
        assertEquals(1, receipt.get("reservationId").asInt());
        assertEquals("DELUXE", receipt.get("roomType").asText());
        assertEquals("2027-03-12", receipt.get("checkOut").asText());
        assertEquals("0.00", receipt.get("discount").decimalValue().setScale(2).toPlainString());
        assertEquals(receipt, JSON.readTree(send("GET", "/reservations/1/document", null).body()));
    }

    @Test
    void errors() throws Exception {
        assertError(send("POST", "/reservations", "não é json"), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", body(201, "2027-03-12", "2027-03-12", 2, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", body(201, "2027-03-10", "2027-03-12", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", body(201, "10/03/2027", "2027-03-12", 2, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", body(201, "2027-03-10", "2027-03-12", 2, "XYZ")), 400, "INVALID_REQUEST");
        assertError(send("POST", "/reservations", body(999, "2027-03-10", "2027-03-12", 2, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(send("POST", "/reservations", body(101, "2027-03-10", "2027-03-12", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(send("GET", "/reservations/99", null), 404, "RESERVATION_NOT_FOUND");
        assertError(send("POST", "/reservations/99/confirmation", null), 404, "RESERVATION_NOT_FOUND");
        assertError(send("GET", "/reports/occupancy?date=2027-13-01", null), 400, "INVALID_REQUEST");
        assertError(send("GET", "/reports/revenue", null), 400, "INVALID_REQUEST");
        assertError(send("GET", "/reports/revenue?month=2027-7", null), 400, "INVALID_REQUEST");
    }

    @Test
    void overlapIsRejectedUnlessCancelledOrBackToBack() throws Exception {
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(send("POST", "/reservations", body(101, "2027-03-11", "2027-03-13", 2, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        create(101, "2027-03-12", "2027-03-14", 2, "STANDARD"); // sai num dia, entra no outro
        send("POST", "/reservations/1/cancellation", null);
        create(101, "2027-03-10", "2027-03-12", 2, "STANDARD");
    }

    @Test
    void reports() throws Exception {
        create(201, "2027-03-10", "2027-03-12", 2, "STANDARD");      // 1
        create(101, "2027-03-11", "2027-03-13", 2, "NON_REFUNDABLE"); // 2
        create(102, "2027-03-11", "2027-03-13", 2, "STANDARD");      // 3 (fica pendente)
        create(202, "2027-03-05", "2027-03-07", 2, "STANDARD");      // 4
        create(301, "2027-04-01", "2027-04-02", 2, "STANDARD");      // 5
        for (int id : new int[]{1, 2, 4, 5}) {
            send("POST", "/reservations/" + id + "/confirmation", null);
        }
        send("POST", "/reservations/2/cancellation", null); // não reembolsável: continua no financeiro
        send("POST", "/reservations/5/cancellation", null); // reembolsado: sai do financeiro

        var occ = send("GET", "/reports/occupancy?date=2027-03-11", null);
        assertEquals("text/csv; charset=utf-8", occ.headers().firstValue("Content-Type").orElse(""));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                occ.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                send("GET", "/reports/occupancy?date=2027-03-12", null).body());

        var rev = send("GET", "/reports/revenue?month=2027-03", null);
        assertEquals("text/csv; charset=utf-8", rev.headers().firstValue("Content-Type").orElse(""));
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                4,202,2027-03-05,2027-03-07,CONFIRMED,779.80
                1,201,2027-03-10,2027-03-12,CONFIRMED,779.80
                2,101,2027-03-11,2027-03-13,CANCELLED,424.83
                total,1984.43
                """, rev.body());
    }
}
