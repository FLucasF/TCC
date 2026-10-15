package br.com.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PedidosApplicationTests {

    private static final TransLogFalsa TRANSLOG;

    static {
        try {
            TRANSLOG = new TransLogFalsa();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) {
        registro.add("translog.url", TRANSLOG::url);
    }

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
    private static final String CPF = "12345678909";

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    int porta;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    record Resposta(int status, JsonNode corpo) {
        String erro() {
            return corpo.path("erro").asString();
        }
    }

    @BeforeEach
    void limpar() throws Exception {
        jdbc.sql("DELETE FROM aviso").update();
        jdbc.sql("DELETE FROM item_pedido").update();
        jdbc.sql("DELETE FROM pedido").update();
        jdbc.sql("DELETE FROM cliente").update();
        jdbc.sql("DELETE FROM produto").update();
        try (var conexao = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conexao, new ClassPathResource("data.sql"));
        }
        TRANSLOG.responder(200, "{\"valor\": 33.10, \"prazoDias\": 2}");
        TRANSLOG.consultas.clear();
    }

    // ---------- resumo ----------

    @Test
    void resumoComPixSemCupom() throws Exception {
        Resposta r = post("/checkout/resumo", compra("EXPRESSA", "PIX", "BRONZE", "SUDESTE", item("CAM-001", 2)));
        assertThat(r.status()).isEqualTo(200);
        assertResumo(r.corpo(), "159.80", "0.00", "33.10", 2, "1.60", "-9.72", "184.78", 1, "184.78", "0.00", false);
        assertThat(TRANSLOG.consultas).containsExactly("cep=01310100&pesoKg=0.60&servico=EXPRESSA");
    }

    @Test
    void freteDaTransLogArredondaMeioParaOParEPrazoDoNordeste() throws Exception {
        TRANSLOG.responder(200, "{\"valor\": 10.125, \"prazoDias\": 2}");
        Resposta r = post("/checkout/resumo", compra("ECONOMICA", "CARTAO", "BRONZE", "NORDESTE", item("MEI-001", 1)));
        assertResumo(r.corpo(), "19.90", "0.00", "10.12", 4, "0.40", "0.00", "30.42", 1, "30.42", "0.00", false);
    }

    @Test
    void seguroDoNorteTemLimiteEPrazoTemTresDiasAMais() throws Exception {
        Resposta r = post("/checkout/resumo", compra("EXPRESSA", "CARTAO", "BRONZE", "NORTE", item("TEN-001", 10)));
        assertThat(r.corpo().get("seguro").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(r.corpo().get("prazoEntregaDias").intValue()).isEqualTo(5);
    }

    @Test
    void cartaoComJurosEOuroNaRetirada() throws Exception {
        ObjectNode dados = compra("RETIRADA_LOJA", "CARTAO", "OURO", "SUDESTE", item("TEN-001", 1));
        dados.put("parcelas", 12);
        Resposta r = post("/checkout/resumo", dados);
        assertResumo(r.corpo(), "249.90", "0.00", "0.00", 1, "2.50", "33.80", "286.20", 12, "23.85", "12.50", false);
    }

    @Test
    void ouroNaoPagaFreteGanhaBrindeMasPagaMotoboy() throws Exception {
        Resposta r = post("/checkout/resumo", compra("EXPRESSA", "PIX", "OURO", "SUDESTE", item("JAQ-001", 2)));
        assertResumo(r.corpo(), "779.80", "0.00", "0.00", 2, "7.80", "-39.38", "748.22", 1, "748.22", "38.99", true);

        Resposta moto = post("/checkout/resumo", compra("MOTOBOY", "PIX", "OURO", "SUDESTE", item("CAM-001", 1)));
        assertThat(moto.corpo().get("frete").decimalValue()).isEqualByComparingTo("18.00");
        assertThat(moto.corpo().get("prazoEntregaDias").intValue()).isZero();
    }

    @Test
    void cupomFreteGratisComBoleto() throws Exception {
        ObjectNode dados = compra("EXPRESSA", "BOLETO", "BRONZE", "SUDESTE", item("CAM-001", 1));
        dados.put("cupom", "FRETEGRATIS");
        Resposta r = post("/checkout/resumo", dados);
        assertResumo(r.corpo(), "79.90", "33.10", "33.10", 2, "0.80", "3.49", "84.19", 1, "84.19", "0.00", false);

        dados.put("formaPagamento", "PIX");
        assertThat(post("/checkout/resumo", dados).corpo().get("ajustePagamento").decimalValue()).isEqualByComparingTo("0");

        dados.put("modalidadeEntrega", "RETIRADA_LOJA");
        assertThat(post("/checkout/resumo", dados).erro()).isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void cupomLeve3Pague2ComPrataEmTresVezes() throws Exception {
        ObjectNode dados = compra("RETIRADA_LOJA", "CARTAO", "PRATA", "SUL", item("CAM-001", 4), item("MEI-001", 6));
        dados.put("cupom", "LEVE3PAGUE2");
        dados.put("parcelas", 3);
        Resposta r = post("/checkout/resumo", dados);
        assertResumo(r.corpo(), "439.00", "119.70", "0.00", 1, "4.39", "0.00", "323.69", 3, "107.90", "8.78", false);
    }

    @Test
    void cupomMenos50() throws Exception {
        ObjectNode dados = compra("RETIRADA_LOJA", "CARTAO", "BRONZE", "SUL", item("CAM-001", 1), item("TEN-001", 1));
        dados.put("cupom", "MENOS50");
        assertThat(post("/checkout/resumo", dados).corpo().get("descontoCupom").decimalValue()).isEqualByComparingTo("50.00");

        ObjectNode pouco = compra("RETIRADA_LOJA", "CARTAO", "BRONZE", "SUL", item("CAM-001", 3));
        pouco.put("cupom", "MENOS50");
        assertThat(post("/checkout/resumo", pouco).erro()).isEqualTo("CUPOM_NAO_APLICAVEL");

        pouco.put("cupom", "menos50");
        assertThat(post("/checkout/resumo", pouco).erro()).isEqualTo("CUPOM_INVALIDO");
    }

    @Test
    void cupomBemVindoSoNaPrimeiraCompra() throws Exception {
        ObjectNode dados = compra("RETIRADA_LOJA", "CARTAO", "BRONZE", "SUDESTE", item("CAM-001", 1));
        dados.put("cupom", "BEMVINDO10");
        assertThat(post("/checkout/resumo", dados).corpo().get("descontoCupom").decimalValue()).isEqualByComparingTo("7.99");

        long cancelado = registrar(compra("RETIRADA_LOJA", "CARTAO", "BRONZE", "SUDESTE", item("CAM-001", 1)));
        pagar(cancelado, "TX-1", Instant.now());
        post("/pedidos/" + cancelado + "/cancelamento", null);
        assertThat(post("/checkout/resumo", dados).status()).isEqualTo(200);

        long pago = registrar(compra("RETIRADA_LOJA", "CARTAO", "BRONZE", "SUDESTE", item("CAM-001", 1)));
        pagar(pago, "TX-2", Instant.now());
        assertThat(post("/checkout/resumo", dados).erro()).isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    // ---------- erros do resumo ----------

    @Test
    void errosDoResumo() throws Exception {
        assertErro(compra("EXPRESSA", "PIX", "BRONZE", "SUDESTE"), 422, "PEDIDO_INVALIDO");
        assertErro(compra("EXPRESSA", "PIX", "BRONZE", "SUDESTE", item("CAM-001", 0)), 422, "PEDIDO_INVALIDO");
        assertErro(compra("EXPRESSA", "PIX", "BRONZE", "SUDESTE", item("CAM-001", 1), item("CAM-001", 1)), 422, "PEDIDO_INVALIDO");
        ObjectNode semCpf = compra("EXPRESSA", "PIX", "BRONZE", "SUDESTE", item("CAM-001", 1));
        ((ObjectNode) semCpf.get("cliente")).remove("cpf");
        assertErro(semCpf, 422, "PEDIDO_INVALIDO");
        assertErro(compra("EXPRESSA", "PIX", "XYZ", "SUDESTE", item("XXX-001", 1)), 422, "PRODUTO_INEXISTENTE");
        assertErro(compra("EXPRESSA", "PIX", "XYZ", "SUDESTE", item("BON-001", 1), item("REL-001", 1)), 422, "PRODUTO_FORA_DE_LINHA");
        assertErro(compra("EXPRESSA", "PIX", "XYZ", "SUDESTE", item("BON-001", 1)), 422, "ESTOQUE_INSUFICIENTE");
        assertErro(compra("EXPRESSA", "PIX", "XYZ", "LUA", item("CAM-001", 1)), 422, "NIVEL_CLUBE_INVALIDO");
        assertErro(compra("EXPRESSA", "PIX", "OURO", "LUA", item("CAM-001", 1)), 422, "REGIAO_INVALIDA");
        ObjectNode cep = compra("XPTO", "PIX", "OURO", "SUL", item("CAM-001", 1));
        cep.put("cep", "01310-100");
        assertErro(cep, 422, "CEP_INVALIDO");
        assertErro(compra("XPTO", "PIX", "OURO", "SUL", item("CAM-001", 1)), 422, "MODALIDADE_INVALIDA");
        assertErro(compra("MOTOBOY", "PIX", "OURO", "SUL", item("TEN-001", 5)), 422, "MODALIDADE_INDISPONIVEL");
        assertThat(post("/checkout/resumo", compra("MOTOBOY", "PIX", "OURO", "SUL", item("MEI-001", 50))).status()).isEqualTo(200);
        ObjectNode cupom = compra("RETIRADA_LOJA", "XPTO", "OURO", "SUL", item("CAM-001", 1));
        cupom.put("cupom", "NAOEXISTE");
        assertErro(cupom, 422, "CUPOM_INVALIDO");
        assertErro(compra("RETIRADA_LOJA", "XPTO", "OURO", "SUL", item("CAM-001", 1)), 422, "FORMA_PAGAMENTO_INVALIDA");
        ObjectNode parcelas = compra("RETIRADA_LOJA", "PIX", "OURO", "SUL", item("CAM-001", 1));
        parcelas.put("parcelas", 2);
        assertErro(parcelas, 422, "PARCELAMENTO_INVALIDO");
        parcelas.put("formaPagamento", "CARTAO");
        parcelas.put("parcelas", 13);
        assertErro(parcelas, 422, "PARCELAMENTO_INVALIDO");
        assertErro(compra("RETIRADA_LOJA", "BOLETO", "BRONZE", "SUL", item("JAQ-001", 3)), 422, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void transLogNaoAtendeOuForaDoAr() throws Exception {
        ObjectNode dados = compra("ECONOMICA", "PIX", "BRONZE", "SUL", item("CAM-001", 1));
        TRANSLOG.responder(404, "");
        assertErro(dados, 422, "MODALIDADE_INDISPONIVEL");
        TRANSLOG.responder(500, "erro");
        assertErro(dados, 503, "TRANSPORTADORA_INDISPONIVEL");
        TRANSLOG.responder(200, "não é json");
        assertErro(dados, 503, "TRANSPORTADORA_INDISPONIVEL");
        TRANSLOG.demorar(2500);
        assertErro(dados, 503, "TRANSPORTADORA_INDISPONIVEL");
    }

    // ---------- pedidos ----------

    @Test
    void fluxoCompletoDoPedido() throws Exception {
        ObjectNode dados = compra("EXPRESSA", "PIX", "PRATA", "SUDESTE", item("CAM-001", 2));
        JsonNode resumo = post("/checkout/resumo", dados).corpo();
        Resposta registrado = post("/pedidos", dados);
        assertThat(registrado.status()).isEqualTo(201);
        assertThat(registrado.corpo().get("resumo")).isEqualTo(resumo);
        assertThat(registrado.corpo().get("situacao").asString()).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(registrado.corpo().get("codigoRastreio").isNull()).isTrue();
        assertThat(registrado.corpo().get("criadoEm").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        long id = registrado.corpo().get("id").longValue();
        assertThat(get("/clientes/" + CPF).corpo().get("saldoCredito").decimalValue()).isEqualByComparingTo("0");

        Resposta pago = pagar(id, "TX-123", Instant.now());
        assertThat(pago.status()).isEqualTo(200);
        assertThat(pago.corpo().get("situacao").asString()).isEqualTo("PAGO");
        assertThat(estoque("CAM-001")).isEqualTo(48);
        assertThat(get("/clientes/" + CPF).corpo().get("saldoCredito").decimalValue()).isEqualByComparingTo("3.20");

        Resposta repetido = pagar(id, "TX-123", Instant.now());
        assertThat(repetido.status()).isEqualTo(200);
        assertThat(estoque("CAM-001")).isEqualTo(48);
        assertThat(get("/clientes/" + CPF).corpo().get("saldoCredito").decimalValue()).isEqualByComparingTo("3.20");
        assertThat(pagar(id, "TX-OUTRA", Instant.now()).erro()).isEqualTo("OPERACAO_NAO_PERMITIDA");

        assertThat(post("/pedidos/" + id + "/entrega", null).erro()).isEqualTo("OPERACAO_NAO_PERMITIDA");
        Resposta enviado = post("/pedidos/" + id + "/envio", JSON.readTree("{\"codigoRastreio\":\"BR123456789\"}"));
        assertThat(enviado.corpo().get("codigoRastreio").asString()).isEqualTo("BR123456789");
        assertThat(post("/pedidos/" + id + "/cancelamento", null).status()).isEqualTo(409);
        Resposta entregue = post("/pedidos/" + id + "/entrega", null);
        assertThat(entregue.corpo().get("situacao").asString()).isEqualTo("ENTREGUE");
        assertThat(get("/pedidos/" + id).corpo().get("resumo")).isEqualTo(resumo);

        JsonNode avisos = get("/clientes/" + CPF + "/avisos").corpo();
        assertThat(avisos.size()).isEqualTo(2);
        assertThat(avisos.get(0).get("tipo").asString()).isEqualTo("PAGAMENTO_APROVADO");
        assertThat(avisos.get(1).get("tipo").asString()).isEqualTo("PEDIDO_ENVIADO");
        assertThat(avisos.get(0).get("situacao").asString()).isEqualTo("NOVO");
        assertThat(avisos.get(0).get("pedidoId").longValue()).isEqualTo(id);
        assertThat(avisos.get(0).has("cpf")).isFalse();

        long avisoId = avisos.get(0).get("id").longValue();
        assertThat(post("/avisos/" + avisoId + "/leitura", null).corpo().get("situacao").asString()).isEqualTo("LIDO");
        assertThat(post("/avisos/" + avisoId + "/leitura", null).corpo().get("situacao").asString()).isEqualTo("LIDO");
        assertThat(post("/avisos/999999/leitura", null).erro()).isEqualTo("AVISO_NAO_ENCONTRADO");
    }

    @Test
    void cancelarPedidoPagoDevolveEstoqueECredito() throws Exception {
        long id = registrar(compra("RETIRADA_LOJA", "PIX", "OURO", "SUDESTE", item("FON-001", 2)));
        pagar(id, "TX-1", Instant.now());
        assertThat(estoque("FON-001")).isEqualTo(3);
        assertThat(get("/clientes/" + CPF).corpo().get("saldoCredito").decimalValue()).isEqualByComparingTo("19.99");

        Resposta cancelado = post("/pedidos/" + id + "/cancelamento", null);
        assertThat(cancelado.corpo().get("situacao").asString()).isEqualTo("CANCELADO");
        assertThat(estoque("FON-001")).isEqualTo(5);
        assertThat(get("/clientes/" + CPF).corpo().get("saldoCredito").decimalValue()).isEqualByComparingTo("0");
        assertThat(post("/pedidos/" + id + "/cancelamento", null).erro()).isEqualTo("OPERACAO_NAO_PERMITIDA");

        long naoPago = registrar(compra("RETIRADA_LOJA", "PIX", "OURO", "SUDESTE", item("FON-001", 2)));
        post("/pedidos/" + naoPago + "/cancelamento", null);
        assertThat(estoque("FON-001")).isEqualTo(5);
        JsonNode avisos = get("/clientes/" + CPF + "/avisos").corpo();
        assertThat(avisos.size()).isEqualTo(3);
        assertThat(avisos.get(2).get("tipo").asString()).isEqualTo("PEDIDO_CANCELADO");
    }

    @Test
    void pagamentoDepoisDe48HorasCancelaOPedido() throws Exception {
        long dentro = registrar(compra("RETIRADA_LOJA", "PIX", "BRONZE", "SUDESTE", item("CAM-001", 1)));
        Instant criado = Instant.parse(get("/pedidos/" + dentro).corpo().get("criadoEm").asString());
        assertThat(pagar(dentro, "TX-1", criado.plusSeconds(48 * 3600)).status()).isEqualTo(200);

        long fora = registrar(compra("RETIRADA_LOJA", "PIX", "BRONZE", "SUDESTE", item("CAM-001", 1)));
        criado = Instant.parse(get("/pedidos/" + fora).corpo().get("criadoEm").asString());
        Resposta expirado = pagar(fora, "TX-2", criado.plusSeconds(48 * 3600 + 1));
        assertThat(expirado.status()).isEqualTo(409);
        assertThat(expirado.erro()).isEqualTo("PEDIDO_EXPIRADO");
        assertThat(get("/pedidos/" + fora).corpo().get("situacao").asString()).isEqualTo("CANCELADO");
        assertThat(estoque("CAM-001")).isEqualTo(49);
        JsonNode avisos = get("/clientes/" + CPF + "/avisos").corpo();
        assertThat(avisos.get(avisos.size() - 1).get("tipo").asString()).isEqualTo("PEDIDO_CANCELADO");
    }

    @Test
    void ultimaPecaPagaAoMesmoTempoSoVendeUmaVez() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ids.add(registrar(compra("RETIRADA_LOJA", "PIX", "BRONZE", "SUDESTE", item("MOC-001", 1))));
        }
        ExecutorService executor = Executors.newFixedThreadPool(ids.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Resposta>> respostas = new ArrayList<>();
        for (long id : ids) {
            Callable<Resposta> tarefa = () -> {
                largada.await();
                return pagar(id, "TX-" + id, Instant.now());
            };
            respostas.add(executor.submit(tarefa));
        }
        largada.countDown();
        int pagos = 0;
        for (Future<Resposta> resposta : respostas) {
            Resposta r = resposta.get();
            if (r.status() == 200) {
                pagos++;
            } else {
                assertThat(r.status()).isEqualTo(409);
                assertThat(r.erro()).isEqualTo("ESTOQUE_INSUFICIENTE");
            }
        }
        executor.shutdown();
        assertThat(pagos).isEqualTo(1);
        assertThat(estoque("MOC-001")).isZero();
    }

    @Test
    void avisoDePagamentoRepetidoAoMesmoTempoContaUmaVez() throws Exception {
        long id = registrar(compra("RETIRADA_LOJA", "PIX", "OURO", "SUDESTE", item("CAM-001", 1)));
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Resposta>> respostas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            respostas.add(executor.submit(() -> {
                largada.await();
                return pagar(id, "TX-9", Instant.now());
            }));
        }
        largada.countDown();
        for (Future<Resposta> resposta : respostas) {
            assertThat(resposta.get().status()).isEqualTo(200);
        }
        executor.shutdown();
        assertThat(estoque("CAM-001")).isEqualTo(49);
        assertThat(get("/clientes/" + CPF).corpo().get("saldoCredito").decimalValue()).isEqualByComparingTo("4.00");
        assertThat(get("/clientes/" + CPF + "/avisos").corpo().size()).isEqualTo(1);
    }

    @Test
    void consultasQueNaoEncontram() throws Exception {
        assertThat(get("/pedidos/999").erro()).isEqualTo("PEDIDO_NAO_ENCONTRADO");
        assertThat(get("/pedidos/abc").status()).isEqualTo(404);
        assertThat(pagar(999, "TX", Instant.now()).erro()).isEqualTo("PEDIDO_NAO_ENCONTRADO");
        assertThat(get("/produtos/XXX").erro()).isEqualTo("PRODUTO_NAO_ENCONTRADO");
        assertThat(get("/clientes/00000000000").erro()).isEqualTo("CLIENTE_NAO_ENCONTRADO");
        assertThat(get("/clientes/00000000000/avisos").erro()).isEqualTo("CLIENTE_NAO_ENCONTRADO");

        JsonNode produto = get("/produtos/CAM-001").corpo();
        assertThat(produto.toString()).isEqualTo(
                "{\"sku\":\"CAM-001\",\"nome\":\"Camiseta básica\",\"preco\":79.90,\"pesoKg\":0.30,\"estoque\":50,\"foraDeLinha\":false}");
    }

    // ---------- apoio ----------

    private static ObjectNode compra(String modalidade, String forma, String nivel, String regiao, ObjectNode... itens) {
        ObjectNode dados = JSON.createObjectNode();
        dados.putObject("cliente").put("cpf", CPF).put("email", "ana@exemplo.com");
        var lista = dados.putArray("itens");
        for (ObjectNode item : itens) {
            lista.add(item);
        }
        dados.put("cep", "01310100");
        dados.put("regiao", regiao);
        dados.put("modalidadeEntrega", modalidade);
        dados.put("formaPagamento", forma);
        dados.put("nivelClube", nivel);
        return dados;
    }

    private static ObjectNode item(String sku, int quantidade) {
        return JSON.createObjectNode().put("sku", sku).put("quantidade", quantidade);
    }

    private static void assertResumo(JsonNode resumo, String subtotal, String desconto, String frete, int prazo,
            String seguro, String ajuste, String total, int parcelas, String parcela, String credito, boolean brinde) {
        assertThat(resumo.toString()).isEqualTo("{\"subtotalProdutos\":" + subtotal + ",\"descontoCupom\":" + desconto
                + ",\"frete\":" + frete + ",\"prazoEntregaDias\":" + prazo + ",\"seguro\":" + seguro
                + ",\"ajustePagamento\":" + ajuste + ",\"totalFinal\":" + total + ",\"parcelas\":" + parcelas
                + ",\"valorParcela\":" + parcela + ",\"creditoProximaCompra\":" + credito + ",\"brinde\":" + brinde + "}");
    }

    private void assertErro(JsonNode dados, int status, String codigo) throws Exception {
        for (String caminho : List.of("/checkout/resumo", "/pedidos")) {
            Resposta r = post(caminho, dados);
            assertThat(r.status()).as(caminho + " " + dados).isEqualTo(status);
            assertThat(r.erro()).as(caminho + " " + dados).isEqualTo(codigo);
        }
    }

    private long registrar(JsonNode dados) throws Exception {
        Resposta r = post("/pedidos", dados);
        assertThat(r.status()).as(r.corpo().toString()).isEqualTo(201);
        return r.corpo().get("id").longValue();
    }

    private Resposta pagar(long id, String transacao, Instant pagoEm) throws Exception {
        ObjectNode corpo = JSON.createObjectNode().put("idTransacao", transacao).put("pagoEm", pagoEm.toString());
        return post("/pedidos/" + id + "/pagamento", corpo);
    }

    private int estoque(String sku) throws Exception {
        return get("/produtos/" + sku).corpo().get("estoque").intValue();
    }

    private Resposta post(String caminho, JsonNode corpo) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json");
        requisicao.POST(corpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo.toString()));
        return enviar(requisicao.build());
    }

    private Resposta get(String caminho) throws Exception {
        return enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build());
    }

    private Resposta enviar(HttpRequest requisicao) throws Exception {
        HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
        String corpo = resposta.body();
        return new Resposta(resposta.statusCode(), corpo.isEmpty() ? JSON.createObjectNode() : JSON.readTree(corpo));
    }
}
