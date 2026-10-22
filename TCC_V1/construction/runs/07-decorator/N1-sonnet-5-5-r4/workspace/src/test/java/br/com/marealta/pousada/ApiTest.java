package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.pousada.messaging.MessageRules;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApiTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private TestSystem system;

    @BeforeAll
    static void requireDatabase() {
        Assumptions.assumeTrue(TestSystem.databaseAvailable(), "PostgreSQL indisponível");
    }

    @AfterEach
    void stop() throws Exception {
        if (system != null) {
            system.close();
        }
    }

    private static String body(int room, String email, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"%s","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, email, in, out, guests, plan);
    }

    private JsonNode reserve(int room, String email, String in, String out, int guests, String plan) throws Exception {
        var response = system.post("/reservations", body(room, email, in, out, guests, plan));
        assertEquals(201, response.status(), response.body());
        return JSON.readTree(response.body());
    }

    private void assertError(TestSystem.Response response, int status, String code) throws Exception {
        assertEquals(status, response.status(), response.body());
        assertEquals(code, JSON.readTree(response.body()).get("error").asText());
    }

    @Test
    void reservationLifecycleAndConfirmationEmail() throws Exception {
        system = TestSystem.start(false, MessageRules.DEFAULT);
        JsonNode created = reserve(201, "carla@exemplo.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertEquals(1, created.get("id").asInt());
        assertEquals("PENDING", created.get("status").asText());
        assertEquals("779.80", created.get("subtotal").decimalValue().toPlainString());
        assertEquals("0.00", created.get("amountPaid").decimalValue().toPlainString());
        assertTrue(created.get("refund").isNull());
        assertError(system.get("/reservations/1/document"), 409, "OPERATION_NOT_ALLOWED");

        JsonNode confirmed = JSON.readTree(system.post("/reservations/1/confirmation", "").body());
        assertEquals("CONFIRMED", confirmed.get("status").asText());
        assertEquals("779.80", confirmed.get("amountPaid").decimalValue().toPlainString());
        assertError(system.post("/reservations/1/confirmation", ""), 409, "OPERATION_NOT_ALLOWED");

        JsonNode messages = JSON.readTree(system.get("/messages").body());
        assertEquals(2, messages.size());
        String receipt = """
                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""";
        String expectedBody = "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. "
                + "Estes são os dados dela:\n\n" + receipt
                + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!";
        JsonNode first = messages.get(0);
        assertEquals("EMAIL", first.get("channel").asText());
        assertEquals("carla@exemplo.com", first.get("to").asText());
        assertEquals("Reserva confirmada - Pousada Maré Alta", first.get("subject").asText());
        assertEquals(expectedBody, first.get("body").asText());
        assertEquals("SENT", first.get("status").asText());
        JsonNode copy = messages.get(1);
        assertEquals("arquivo@pousadamarealta.com.br", copy.get("to").asText());
        assertEquals("[Cópia] Reserva confirmada - Pousada Maré Alta", copy.get("subject").asText());
        assertEquals(expectedBody, copy.get("body").asText());

        JsonNode checkedIn = JSON.readTree(system.post("/reservations/1/check-in", "").body());
        assertEquals("CHECKED_IN", checkedIn.get("status").asText());
        assertError(system.post("/reservations/1/cancellation", ""), 409, "OPERATION_NOT_ALLOWED");
        JsonNode out = JSON.readTree(system.post("/reservations/1/check-out", "").body());
        assertEquals("Comprovante de reserva", out.get("title").asText());
        assertEquals("DELUXE", out.get("roomType").asText());
        assertEquals("2027-03-12", out.get("checkOut").asText());
        assertEquals("779.80", out.get("total").decimalValue().toPlainString());
        assertEquals(out, JSON.readTree(system.get("/reservations/1/document").body()));
    }

    @Test
    void invalidAddressIsBlockedAndTestModeRedirects() throws Exception {
        system = TestSystem.start(false, MessageRules.DEFAULT);
        reserve(101, "ana@gmail", "2027-03-10", "2027-03-11", 1, "STANDARD");
        system.post("/reservations/1/confirmation", "");
        JsonNode messages = JSON.readTree(system.get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("BLOCKED", messages.get(0).get("status").asText());
        assertEquals("ana@gmail", messages.get(0).get("to").asText());
        system.close();

        system = TestSystem.start(true, MessageRules.DEFAULT);
        reserve(101, "ana@gmail.com", "2027-03-10", "2027-03-11", 1, "STANDARD");
        system.post("/reservations/1/confirmation", "");
        messages = JSON.readTree(system.get("/messages").body());
        assertEquals(1, messages.size());
        assertEquals("testes@pousadamarealta.com.br", messages.get(0).get("to").asText());
        assertEquals("[TESTE] Reserva confirmada - Pousada Maré Alta", messages.get(0).get("subject").asText());
        assertTrue(messages.get(0).get("body").asText().startsWith("Destinatário original: ana@gmail.com\nOlá, Carla Dias!"));
    }

    @Test
    void validationAndConflicts() throws Exception {
        system = TestSystem.start(false, MessageRules.DEFAULT);
        assertEquals(5, JSON.readTree(system.get("/rooms").body()).size());
        assertEquals("249.90", JSON.readTree(system.get("/rooms").body()).get(0).get("nightlyRate").decimalValue().toPlainString());
        assertError(system.post("/reservations", "nao e json"), 400, "INVALID_REQUEST");
        assertError(system.post("/reservations", "{}"), 400, "INVALID_REQUEST");
        assertError(system.post("/reservations", body(101, "a@b.com", "2027-03-10", "2027-03-10", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(system.post("/reservations", body(101, "a@b.com", "2027-03-10", "2027-03-11", 0, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(system.post("/reservations", body(101, "a@b.com", "10/03/2027", "2027-03-11", 1, "STANDARD")), 400, "INVALID_REQUEST");
        assertError(system.post("/reservations", body(999, "a@b.com", "2027-03-10", "2027-03-11", 1, "STANDARD")), 404, "ROOM_NOT_FOUND");
        assertError(system.post("/reservations", body(101, "a@b.com", "2027-03-10", "2027-03-11", 3, "STANDARD")), 422, "CAPACITY_EXCEEDED");
        assertError(system.get("/reservations/99"), 404, "RESERVATION_NOT_FOUND");
        assertError(system.post("/reservations/99/check-in", ""), 404, "RESERVATION_NOT_FOUND");

        reserve(101, "a@b.com", "2027-03-10", "2027-03-12", 2, "STANDARD");
        assertError(system.post("/reservations", body(101, "a@b.com", "2027-03-11", "2027-03-13", 1, "STANDARD")), 409, "ROOM_UNAVAILABLE");
        reserve(101, "a@b.com", "2027-03-12", "2027-03-13", 1, "STANDARD");
        system.post("/reservations/1/cancellation", "");
        reserve(101, "a@b.com", "2027-03-10", "2027-03-11", 1, "STANDARD");
        assertError(system.get("/reports/occupancy?date=ontem"), 400, "INVALID_REQUEST");
        assertError(system.get("/reports/revenue"), 400, "INVALID_REQUEST");
    }

    @Test
    void cancellationRefundsAndReports() throws Exception {
        system = TestSystem.start(false, MessageRules.DEFAULT);
        reserve(101, "a@b.com", "2027-07-10", "2027-07-12", 2, "STANDARD"); // 1: cancelada paga
        reserve(102, "a@b.com", "2027-07-10", "2027-07-12", 2, "NON_REFUNDABLE"); // 2: cancelada não reembolsável
        reserve(201, "a@b.com", "2027-07-09", "2027-07-10", 2, "STANDARD"); // 3: sai no dia
        reserve(202, "a@b.com", "2027-07-10", "2027-07-11", 2, "STANDARD"); // 4: pendente
        reserve(301, "a@b.com", "2027-07-10", "2027-07-11", 4, "NON_REFUNDABLE"); // 5: ocupa
        reserve(201, "a@b.com", "2027-07-10", "2027-07-11", 2, "STANDARD"); // 6: ocupa
        for (int id : new int[] {1, 2, 3, 5, 6}) {
            system.post("/reservations/" + id + "/confirmation", "");
        }
        JsonNode one = JSON.readTree(system.post("/reservations/1/cancellation", "").body());
        assertEquals("599.76", one.get("refund").decimalValue().toPlainString());
        assertEquals("599.76", one.get("amountPaid").decimalValue().toPlainString());
        JsonNode two = JSON.readTree(system.post("/reservations/2/cancellation", "").body());
        assertEquals("0.00", two.get("refund").decimalValue().toPlainString());
        assertEquals("509.80", two.get("amountPaid").decimalValue().toPlainString());
        JsonNode four = JSON.readTree(system.post("/reservations/4/cancellation", "").body());
        assertEquals("0.00", four.get("refund").decimalValue().toPlainString());
        assertError(system.get("/reservations/4/document"), 409, "OPERATION_NOT_ALLOWED");

        var occupancy = system.get("/reports/occupancy?date=2027-07-10");
        assertEquals("text/csv; charset=utf-8", occupancy.contentType().toLowerCase());
        assertEquals("""
                room,type,reservation,check_in,check_out
                201,DELUXE,6,2027-07-10,2027-07-11
                301,SUITE,5,2027-07-10,2027-07-11
                total,2/5
                """, occupancy.body());

        var revenue = system.get("/reports/revenue?month=2027-07");
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                3,201,2027-07-09,2027-07-10,CONFIRMED,467.88
                2,102,2027-07-10,2027-07-12,CANCELLED,509.80
                5,301,2027-07-10,2027-07-11,CONFIRMED,591.50
                6,201,2027-07-10,2027-07-11,CONFIRMED,467.88
                total,2037.06
                """, revenue.body());
    }
}
