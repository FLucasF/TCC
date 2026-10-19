package br.com.marealta;

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

import static org.junit.jupiter.api.Assertions.*;

class PousadaTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(com.fasterxml.jackson.databind.node.JsonNodeFactory.withExactBigDecimals(true));
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void subir() {
        app = Aplicacao.criar().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void parar() {
        app.stop();
    }

    private HttpResponse<String> chamar(String metodo, String caminho, String corpo) throws Exception {
        HttpRequest.BodyPublisher b = corpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo);
        return http.send(HttpRequest.newBuilder(URI.create(base + caminho)).method(metodo, b).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> r) throws Exception {
        return mapper.readTree(r.body());
    }

    private String pedido(int quarto, String entrada, String saida, int hospedes, String plano) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(quarto, entrada, saida, hospedes, plano);
    }

    private JsonNode criar(int quarto, String entrada, String saida, String plano) throws Exception {
        HttpResponse<String> r = chamar("POST", "/reservations", pedido(quarto, entrada, saida, 2, plano));
        assertEquals(201, r.statusCode());
        return json(r);
    }

    @Test
    void precoDeAltaTemporada() throws Exception {
        JsonNode r = criar(101, "2027-07-10", "2027-07-12", "STANDARD");
        assertEquals(1, r.get("id").asInt());
        assertEquals("599.76", r.get("total").decimalValue().toPlainString());
        assertEquals("0.00", r.get("discount").decimalValue().toPlainString());
        assertEquals("PENDING", r.get("status").asText());
        assertTrue(r.get("refund").isNull());
    }

    @Test
    void confirmacaoGeraComprovanteEEmails() throws Exception {
        criar(201, "2027-03-10", "2027-03-12", "STANDARD");
        assertEquals(200, chamar("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode msgs = json(chamar("GET", "/messages", null));
        assertEquals(3, msgs.size());
        JsonNode hospede = msgs.get(0);
        assertEquals("Reserva confirmada - Pousada Maré Alta", hospede.get("subject").asText());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", hospede.get("body").asText());
        assertEquals("governanca@pousadamarealta.com.br", msgs.get(1).get("to").asText());
        assertEquals("Reserva confirmada - quarto 201", msgs.get(1).get("subject").asText());
        assertEquals("Pagamento recebido - reserva 1", msgs.get(2).get("subject").asText());
        assertTrue(msgs.get(2).get("body").asText().contains("R$ 779,80"));
    }

    @Test
    void cancelamentoDeConfirmadaReembolsavelAvisaGovernancaEFinanceiro() throws Exception {
        criar(201, "2027-03-10", "2027-03-12", "STANDARD");
        chamar("POST", "/reservations/1/confirmation", null);
        JsonNode r = json(chamar("POST", "/reservations/1/cancellation", null));
        assertEquals("779.80", r.get("refund").decimalValue().toPlainString());
        JsonNode msgs = json(chamar("GET", "/messages", null));
        assertEquals(5, msgs.size());
        assertEquals("Reserva cancelada - quarto 201", msgs.get(3).get("subject").asText());
        assertEquals("Reembolso - reserva 1", msgs.get(4).get("subject").asText());
    }

    @Test
    void naoReembolsavelCanceladaSoAvisaGovernancaEContinuaNoFinanceiro() throws Exception {
        criar(101, "2027-03-10", "2027-03-12", "NON_REFUNDABLE");
        chamar("POST", "/reservations/1/confirmation", null);
        chamar("POST", "/reservations/1/cancellation", null);
        assertEquals(4, json(chamar("GET", "/messages", null)).size());
        HttpResponse<String> csv = chamar("GET", "/reports/revenue?month=2027-03", null);
        assertTrue(csv.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertEquals("""
                reservation,room,check_in,check_out,status,amount
                1,101,2027-03-10,2027-03-12,CANCELLED,424.83
                total,424.83
                """, csv.body());
    }

    @Test
    void cancelarPendenteNaoAvisaNinguem() throws Exception {
        criar(101, "2027-03-10", "2027-03-12", "STANDARD");
        chamar("POST", "/reservations/1/cancellation", null);
        assertEquals(0, json(chamar("GET", "/messages", null)).size());
    }

    @Test
    void fluxoCompletoERelatorioDeOcupacao() throws Exception {
        criar(301, "2027-03-10", "2027-03-12", "STANDARD");
        assertEquals(409, chamar("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, chamar("GET", "/reservations/1/document", null).statusCode());
        chamar("POST", "/reservations/1/confirmation", null);
        assertEquals(409, chamar("POST", "/reservations/1/confirmation", null).statusCode());
        assertEquals("""
                room,type,reservation,check_in,check_out
                301,SUITE,1,2027-03-10,2027-03-12
                total,1/5
                """, chamar("GET", "/reports/occupancy?date=2027-03-11", null).body());
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                chamar("GET", "/reports/occupancy?date=2027-03-12", null).body());
        chamar("POST", "/reservations/1/check-in", null);
        JsonNode doc = json(chamar("POST", "/reservations/1/check-out", null));
        assertEquals("Comprovante de reserva", doc.get("title").asText());
        assertEquals("SUITE", doc.get("roomType").asText());
        assertEquals(200, chamar("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, chamar("POST", "/reservations/1/cancellation", null).statusCode());
    }

    @Test
    void erros() throws Exception {
        criar(101, "2027-03-10", "2027-03-12", "STANDARD");
        assertEquals("ROOM_UNAVAILABLE", json(chamar("POST", "/reservations",
                pedido(101, "2027-03-11", "2027-03-13", 1, "STANDARD"))).get("error").asText());
        assertEquals(201, chamar("POST", "/reservations",
                pedido(101, "2027-03-12", "2027-03-13", 1, "STANDARD")).statusCode());
        assertEquals(422, chamar("POST", "/reservations", pedido(101, "2027-05-01", "2027-05-02", 3, "STANDARD")).statusCode());
        assertEquals(404, chamar("POST", "/reservations", pedido(999, "2027-05-01", "2027-05-02", 1, "STANDARD")).statusCode());
        assertEquals(400, chamar("POST", "/reservations", pedido(101, "2027-05-02", "2027-05-02", 1, "STANDARD")).statusCode());
        assertEquals(400, chamar("POST", "/reservations", pedido(101, "2027-05-01", "2027-05-02", 0, "STANDARD")).statusCode());
        assertEquals(400, chamar("POST", "/reservations", "não é json").statusCode());
        assertEquals(400, chamar("POST", "/reservations", "{}").statusCode());
        assertEquals(404, chamar("GET", "/reservations/99", null).statusCode());
        assertEquals("RESERVATION_NOT_FOUND", json(chamar("POST", "/reservations/abc/confirmation", null)).get("error").asText());
        assertEquals(400, chamar("GET", "/reports/occupancy?date=10-07-2027", null).statusCode());
        assertEquals(400, chamar("GET", "/reports/revenue", null).statusCode());
    }

    @Test
    void quartos() throws Exception {
        JsonNode q = json(chamar("GET", "/rooms", null));
        assertEquals(5, q.size());
        assertEquals(579.90, q.get(4).get("nightlyRate").asDouble());
        assertEquals("SUITE", q.get(4).get("type").asText());
    }
}
