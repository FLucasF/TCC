package com.pousadamaretalta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppTest {
    Javalin app;
    HttpClient http = HttpClient.newHttpClient();
    String base;

    @BeforeEach
    void start() {
        app = App.create(new ReservationService()).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode json(HttpResponse<String> r) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(r.body());
    }

    static String body(int room, String ident, String in, String out, int guests, String plan) {
        return "{\"roomNumber\":%d,\"guestName\":\"Carla Dias\",\"guestEmail\":\"carla@x.com\",%s,\"checkIn\":\"%s\",\"checkOut\":\"%s\",\"guests\":%d,\"ratePlan\":\"%s\"}"
                .formatted(room, ident, in, out, guests, plan);
    }

    static final String CPF = "\"guestCpf\":\"111.222.333-44\"";

    @Test
    void highSeasonPricing() throws Exception {
        var r = call("POST", "/reservations", body(101, CPF, "2027-07-10", "2027-07-12", 2, "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"total\":599.76,"));
        assertTrue(r.body().contains("\"amountPaid\":0.00,"));
        assertTrue(json(r).get("refund").isNull());
        assertTrue(json(r).get("companyCnpj").isNull());
    }

    @Test
    void confirmationSendsEmailAndFullLifecycle() throws Exception {
        call("POST", "/reservations", body(201, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        var c = call("POST", "/reservations/1/confirmation", null);
        assertTrue(c.body().contains("\"amountPaid\":779.80,"));
        var msgs = json(call("GET", "/messages", null));
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n"
                + "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", msgs.get(0).get("body").asText());
        assertEquals(409, call("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-out", null).statusCode());
        assertEquals(200, call("POST", "/reservations/1/check-in", null).statusCode());
        var out = call("POST", "/reservations/1/check-out", null);
        assertEquals("Comprovante de reserva", json(out).get("title").asText());
        assertEquals(json(out).get("text").asText(), json(call("GET", "/reservations/1/document", null)).get("text").asText());
    }

    @Test
    void invoiceAndBookingReceipt() throws Exception {
        call("POST", "/reservations", body(201, "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"",
                "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        String t = json(call("GET", "/reservations/1/document", null)).get("text").asText();
        assertEquals("Fatura de hospedagem nº 1\nEmpresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)\nHóspede: Carla Dias\n"
                + "Quarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\n"
                + "Total: R$ 779,80\nISS retido (5%): R$ 38,99", t);

        call("POST", "/reservations", body(202, "\"guestPassport\":\"X1234567\",\"guestCountry\":\"United Kingdom\"",
                "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/2/confirmation", null);
        String e = json(call("GET", "/reservations/2/document", null)).get("text").asText();
        assertEquals("Booking receipt no. 2\nGuest: Carla Dias (passport X1234567, United Kingdom)\nRoom: 202 (DELUXE)\n"
                + "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights\nNights: BRL 779.80\nDiscount: BRL 116.97\n"
                + "Total: BRL 662.83", e);
    }

    @Test
    void validationAndConflicts() throws Exception {
        assertEquals(400, call("POST", "/reservations", "nope").statusCode());
        assertEquals(400, call("POST", "/reservations", body(101, CPF + ",\"companyCnpj\":\"1\",\"companyName\":\"A\"", "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", body(101, "\"companyCnpj\":\"1\"", "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2027-03-12", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", body(101, CPF, "2027-03-10", "2027-03-12", 0, "STANDARD")).statusCode());
        assertEquals(404, call("POST", "/reservations", body(999, CPF, "2027-03-10", "2027-03-12", 1, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", body(101, CPF, "2027-03-10", "2027-03-12", 3, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", body(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD")).statusCode());
        assertEquals(409, call("POST", "/reservations", body(101, CPF, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(201, call("POST", "/reservations", body(101, CPF, "2027-03-12", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(404, call("GET", "/reservations/99", null).statusCode());
        assertEquals("RESERVATION_NOT_FOUND", json(call("GET", "/reservations/abc", null)).get("error").asText());
    }

    @Test
    void cancellationRefundsAndReports() throws Exception {
        call("POST", "/reservations", body(101, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations", body(102, CPF, "2027-03-10", "2027-03-12", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/2/confirmation", null);
        var c1 = call("POST", "/reservations/1/cancellation", null);
        assertTrue(c1.body().contains("\"refund\":499.80"));
        var c2 = call("POST", "/reservations/2/cancellation", null);
        assertTrue(c2.body().contains("\"refund\":0.00"));
        assertEquals(409, call("POST", "/reservations/2/cancellation", null).statusCode());
        assertEquals(409, call("GET", "/reservations/2/document", null).statusCode());

        var rev = call("GET", "/reports/revenue?month=2027-03", null);
        assertTrue(rev.headers().firstValue("content-type").get().contains("text/csv"));
        assertEquals("reservation,room,check_in,check_out,status,amount\n2,102,2027-03-10,2027-03-12,CANCELLED,424.83\ntotal,424.83\n", rev.body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-10", null).body());
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).statusCode());
    }

    @Test
    void occupancy() throws Exception {
        call("POST", "/reservations", body(202, CPF, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        assertEquals("room,type,reservation,check_in,check_out\n202,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                call("GET", "/reports/occupancy?date=2027-03-11", null).body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                call("GET", "/reports/occupancy?date=2027-03-12", null).body());
    }
}
