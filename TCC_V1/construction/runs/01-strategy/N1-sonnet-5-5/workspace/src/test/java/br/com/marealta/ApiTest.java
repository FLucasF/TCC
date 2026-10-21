package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.web.Rotas;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest {

    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;

    @BeforeEach
    void subir() {
        app = new Rotas().criar().start(0);
    }

    @AfterEach
    void parar() {
        app.stop();
    }

    private HttpResponse<String> chamar(String metodo, String caminho, String corpo) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + caminho))
                .method(metodo, corpo == null ? BodyPublishers.noBody() : BodyPublishers.ofString(corpo))
                .build();
        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private static String pedido(int quarto, String entrada, String saida, int hospedes, String plano, String extra) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@exemplo.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"%s}"""
                .formatted(quarto, entrada, saida, hospedes, plano, extra);
    }

    @Test
    void fluxoCompleto() throws Exception {
        var criada = chamar("POST", "/reservations",
                pedido(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", ",\"companyCode\":\"TECNOSUL\""));
        assertEquals(201, criada.statusCode());
        JsonNode reserva = json.readTree(criada.body());
        assertEquals("686.22", reserva.get("total").asText());
        assertEquals("0.00", reserva.get("amountPaid").asText());
        assertTrue(reserva.get("refund").isNull());

        assertEquals(409, chamar("GET", "/reservations/1/document", null).statusCode());
        var confirmada = json.readTree(chamar("POST", "/reservations/1/confirmation", null).body());
        assertEquals("CONFIRMED", confirmada.get("status").asText());
        assertEquals("686.22", confirmada.get("amountPaid").asText());

        JsonNode mensagens = json.readTree(chamar("GET", "/messages", null).body());
        assertEquals(1, mensagens.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", mensagens.get(0).get("subject").asText());
        assertTrue(mensagens.get(0).get("body").asText().contains("Desconto: R$ 93,58\nTotal: R$ 686,22\n\nA entrada"));

        var ocupacao = chamar("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(ocupacao.headers().firstValue("Content-Type").orElse("").contains("text/csv"));
        assertEquals("room,type,reservation,check_in,check_out\n201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n",
                ocupacao.body());
        assertTrue(chamar("GET", "/reports/occupancy?date=2027-03-12", null).body().endsWith("total,0/5\n"));

        assertEquals(200, chamar("POST", "/reservations/1/check-in", null).statusCode());
        var saida = json.readTree(chamar("POST", "/reservations/1/check-out", null).body());
        assertEquals("Comprovante de reserva", saida.get("title").asText());
        assertEquals("93.58", saida.get("discount").asText());
        assertEquals(409, chamar("POST", "/reservations/1/cancellation", null).statusCode());

        assertEquals("reservation,room,check_in,check_out,status,amount\n1,201,2027-03-10,2027-03-12,CHECKED_OUT,686.22\ntotal,686.22\n",
                chamar("GET", "/reports/revenue?month=2027-03", null).body());
    }

    @Test
    void cancelamentoDoPacote() throws Exception {
        chamar("POST", "/reservations", pedido(201, "2027-03-12", "2027-03-14", 3, "WEEKEND_PACKAGE", ""));
        chamar("POST", "/reservations/1/confirmation", null);
        var cancelada = json.readTree(chamar("POST", "/reservations/1/cancellation", null).body());
        assertEquals("410.00", cancelada.get("refund").asText());
        assertTrue(chamar("GET", "/reports/revenue?month=2027-03", null).body().endsWith("total,410.00\n"));
    }

    @Test
    void erros() throws Exception {
        assertEquals(400, chamar("POST", "/reservations", "nao e json").statusCode());
        assertEquals(400, chamar("POST", "/reservations",
                pedido(201, "2027-03-10", "2027-03-12", 2, "CORPORATE", "")).statusCode());
        assertEquals(400, chamar("POST", "/reservations",
                pedido(201, "2027-03-12", "2027-03-12", 2, "STANDARD", "")).statusCode());
        assertEquals(400, chamar("POST", "/reservations",
                pedido(201, "2027-03-10", "2027-03-12", 0, "STANDARD", "")).statusCode());
        assertEquals(404, chamar("POST", "/reservations",
                pedido(999, "2027-03-10", "2027-03-12", 2, "STANDARD", "")).statusCode());
        assertEquals(422, chamar("POST", "/reservations",
                pedido(101, "2027-03-10", "2027-03-12", 3, "STANDARD", "")).statusCode());
        assertEquals(201, chamar("POST", "/reservations",
                pedido(101, "2027-03-10", "2027-03-12", 2, "STANDARD", ",\"companyCode\":\"x\"")).statusCode());
        var conflito = chamar("POST", "/reservations", pedido(101, "2027-03-11", "2027-03-13", 2, "STANDARD", ""));
        assertEquals(409, conflito.statusCode());
        assertEquals("ROOM_UNAVAILABLE", json.readTree(conflito.body()).get("error").asText());
        assertEquals(404, chamar("GET", "/reservations/77", null).statusCode());
        assertEquals(400, chamar("GET", "/reports/revenue?month=2027-13", null).statusCode());
        assertEquals(400, chamar("GET", "/reports/occupancy", null).statusCode());
        // pendente cancelada: nada a devolver, e libera o quarto
        var cancelada = json.readTree(chamar("POST", "/reservations/1/cancellation", null).body());
        assertEquals("0.00", cancelada.get("refund").asText());
        assertEquals(201, chamar("POST", "/reservations",
                pedido(101, "2027-03-11", "2027-03-13", 2, "STANDARD", "")).statusCode());
    }
}
