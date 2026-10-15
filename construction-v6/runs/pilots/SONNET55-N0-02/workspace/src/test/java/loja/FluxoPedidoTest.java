package loja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.sun.net.httpserver.HttpServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:loja;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=" })
class FluxoPedidoTest {

    static final HttpServer TRANSLOG = subir();

    static HttpServer subir() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/cotacao", ex -> {
                String q = ex.getRequestURI().getQuery();
                int status = q.contains("cep=99999999") ? 404 : q.contains("cep=88888888") ? 500 : 200;
                byte[] body = status == 200 ? "{\"valor\":33.105,\"prazoDias\":2}".getBytes() : new byte[0];
                ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
                if (body.length > 0) {
                    ex.getResponseBody().write(body);
                }
                ex.close();
            });
            s.start();
            return s;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("translog.url", () -> "http://127.0.0.1:" + TRANSLOG.getAddress().getPort());
    }

    @Value("${local.server.port}")
    int porta;

    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder()
            .enable(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

    record Resp(int status, JsonNode corpo, String raw) {
    }

    Resp chamar(String metodo, String caminho, String corpo) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json")
                .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo));
        var r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resp(r.statusCode(), json.readTree(r.body()), r.body());
    }

    String compra(String cpf, String sku, int qtd, String entrega, String cep, String cupom, String pagto,
            int parcelas, String nivel, String regiao) {
        return """
                {"cliente":{"cpf":"%s","email":"a@b.com"},"itens":[{"sku":"%s","quantidade":%d}],
                 "cep":"%s","regiao":"%s","modalidadeEntrega":"%s",%s"formaPagamento":"%s","parcelas":%d,
                 "nivelClube":"%s"}"""
                .formatted(cpf, sku, qtd, cep, regiao, entrega, cupom == null ? "" : "\"cupom\":\"" + cupom + "\",",
                        pagto, parcelas, nivel);
    }

    String simples(String cpf, String sku, int qtd, String nivel) {
        return compra(cpf, sku, qtd, "RETIRADA_LOJA", "01310100", null, "BOLETO", 1, nivel, "SUDESTE");
    }

    @Test
    void resumoComTransportadoraEPix() throws Exception {
        Resp r = chamar("POST", "/checkout/resumo",
                compra("1", "CAM-001", 2, "EXPRESSA", "01310100", null, "PIX", 1, "BRONZE", "NORDESTE"));
        assertEquals(200, r.status());
        assertEquals("159.80", r.corpo().get("subtotalProdutos").decimalValue().toPlainString());
        assertEquals("33.10", r.corpo().get("frete").decimalValue().toPlainString());
        assertEquals(4, r.corpo().get("prazoEntregaDias").asInt());
        assertEquals("3.20", r.corpo().get("seguro").decimalValue().toPlainString());
        // total 196.10 -> 5% = 9.805 -> 9.80
        assertEquals("-9.80", r.corpo().get("ajustePagamento").decimalValue().toPlainString());
        assertEquals("186.30", r.corpo().get("totalFinal").decimalValue().toPlainString());
        assertTrue(r.raw().contains("\"subtotalProdutos\":159.80"), r.raw());
    }

    @Test
    void cartaoComJurosTabelaPrice() throws Exception {
        Resp r = chamar("POST", "/checkout/resumo",
                compra("1", "TEN-001", 1, "MOTOBOY", "01310100", null, "CARTAO", 6, "PRATA", "SUL"));
        assertEquals(200, r.status());
        assertEquals(6, r.corpo().get("parcelas").asInt());
        assertEquals("5.00", r.corpo().get("creditoProximaCompra").decimalValue().toPlainString());
        var parcela = r.corpo().get("valorParcela").decimalValue();
        assertEquals(parcela.multiply(new java.math.BigDecimal(6)).setScale(2).toPlainString(),
                r.corpo().get("totalFinal").decimalValue().toPlainString());
        assertEquals("48.26", parcela.toPlainString()); // 270.40 * 0.0199 / (1-1.0199^-6)
    }

    @Test
    void errosDeEntrega() throws Exception {
        Resp r = chamar("POST", "/checkout/resumo",
                compra("1", "CAM-001", 1, "ECONOMICA", "99999999", null, "PIX", 1, "BRONZE", "SUL"));
        assertEquals(422, r.status());
        assertEquals("MODALIDADE_INDISPONIVEL", r.corpo().get("erro").asString());
        r = chamar("POST", "/checkout/resumo",
                compra("1", "CAM-001", 1, "ECONOMICA", "88888888", null, "PIX", 1, "BRONZE", "SUL"));
        assertEquals(503, r.status());
        r = chamar("POST", "/checkout/resumo",
                compra("1", "REL-001", 1, "ECONOMICA", "88888888", null, "PIX", 1, "BRONZE", "SUL"));
        assertEquals("PRODUTO_FORA_DE_LINHA", r.corpo().get("erro").asString());
    }

    @Test
    void ultimaPecaVendidaUmaVezSo() throws Exception {
        String a = chamar("POST", "/pedidos", simples("111", "MOC-001", 1, "OURO")).corpo().get("id").asString();
        Resp b = chamar("POST", "/pedidos", simples("222", "MOC-001", 1, "BRONZE"));
        assertEquals(201, b.status());
        String idB = b.corpo().get("id").asString();
        String pg = "{\"idTransacao\":\"TX-1\",\"pagoEm\":\"" + Instant.now() + "\"}";

        assertEquals(200, chamar("POST", "/pedidos/" + a + "/pagamento", pg).status());
        assertEquals(200, chamar("POST", "/pedidos/" + a + "/pagamento", pg).status()); // repetido
        assertEquals("0", chamar("GET", "/produtos/MOC-001", null).corpo().get("estoque").asString());
        assertEquals("8.00", chamar("GET", "/clientes/111", null).corpo().get("saldoCredito").decimalValue()
                .toPlainString());
        assertEquals(1, chamar("GET", "/clientes/111/avisos", null).corpo().size());

        Resp falha = chamar("POST", "/pedidos/" + idB + "/pagamento", pg);
        assertEquals(409, falha.status());
        assertEquals("ESTOQUE_INSUFICIENTE", falha.corpo().get("erro").asString());
        assertEquals("CANCELADO", chamar("GET", "/pedidos/" + idB, null).corpo().get("situacao").asString());
        JsonNode avisos = chamar("GET", "/clientes/222/avisos", null).corpo();
        assertEquals("PEDIDO_CANCELADO", avisos.get(0).get("tipo").asString());
        long avisoId = avisos.get(0).get("id").asLong();
        assertEquals("LIDO", chamar("POST", "/avisos/" + avisoId + "/leitura", null).corpo().get("situacao").asString());

        // cancelar o pedido pago devolve estoque e crédito
        assertEquals(200, chamar("POST", "/pedidos/" + a + "/cancelamento", null).status());
        assertEquals("1", chamar("GET", "/produtos/MOC-001", null).corpo().get("estoque").asString());
        assertEquals("0.00", chamar("GET", "/clientes/111", null).corpo().get("saldoCredito").decimalValue()
                .toPlainString());
    }

    @Test
    void ciclosEPrazoDoPagamento() throws Exception {
        Resp p = chamar("POST", "/pedidos", simples("333", "MEI-001", 2, "BRONZE"));
        String id = p.corpo().get("id").asString();
        assertTrue(p.corpo().get("codigoRastreio").isNull());
        assertEquals(409, chamar("POST", "/pedidos/" + id + "/envio", "{\"codigoRastreio\":\"BR1\"}").status());
        Instant criado = Instant.parse(p.corpo().get("criadoEm").asString());
        String pg = "{\"idTransacao\":\"TX-9\",\"pagoEm\":\"" + criado.plus(48, ChronoUnit.HOURS) + "\"}";
        assertEquals(200, chamar("POST", "/pedidos/" + id + "/pagamento", pg).status());
        assertEquals("ENVIADO", chamar("POST", "/pedidos/" + id + "/envio", "{\"codigoRastreio\":\"BR1\"}")
                .corpo().get("situacao").asString());
        assertEquals(409, chamar("POST", "/pedidos/" + id + "/cancelamento", null).status());
        assertEquals("ENTREGUE", chamar("POST", "/pedidos/" + id + "/entrega", null).corpo().get("situacao").asString());

        String id2 = chamar("POST", "/pedidos", simples("334", "MEI-001", 1, "BRONZE")).corpo().get("id").asString();
        String tarde = "{\"idTransacao\":\"TX-8\",\"pagoEm\":\"" + Instant.now().plus(49, ChronoUnit.HOURS) + "\"}";
        Resp e = chamar("POST", "/pedidos/" + id2 + "/pagamento", tarde);
        assertEquals("PEDIDO_EXPIRADO", e.corpo().get("erro").asString());
        assertEquals("CANCELADO", chamar("GET", "/pedidos/" + id2, null).corpo().get("situacao").asString());
        assertEquals(404, chamar("GET", "/pedidos/nao-existe", null).status());
    }

    @Test
    void cuponsPrimeiraCompra() throws Exception {
        String corpo = compra("444", "CAM-001", 3, "RETIRADA_LOJA", "01310100", "LEVE3PAGUE2", "BOLETO", 1,
                "BRONZE", "SUDESTE");
        assertEquals("79.90", chamar("POST", "/checkout/resumo", corpo).corpo().get("descontoCupom").decimalValue()
                .toPlainString());
        String bv = compra("444", "CAM-001", 1, "RETIRADA_LOJA", "01310100", "BEMVINDO10", "BOLETO", 1, "BRONZE",
                "SUDESTE");
        assertEquals("7.99", chamar("POST", "/checkout/resumo", bv).corpo().get("descontoCupom").decimalValue()
                .toPlainString());
        String id = chamar("POST", "/pedidos", bv).corpo().get("id").asString();
        chamar("POST", "/pedidos/" + id + "/pagamento", "{\"idTransacao\":\"T\",\"pagoEm\":\"" + Instant.now() + "\"}");
        assertEquals("CUPOM_NAO_APLICAVEL", chamar("POST", "/checkout/resumo", bv).corpo().get("erro").asString());
    }
}
