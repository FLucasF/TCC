package com.loja.pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
class PedidoApiTest {

    @Autowired
    private WebApplicationContext contexto;

    private final ObjectMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private JsonNode criar(String valorProdutos, String frete) throws Exception {
        String corpo = "{\"valorProdutos\": %s, \"frete\": %s}".formatted(valorProdutos, frete);
        var resposta = mvc().perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn().getResponse();
        assertThat(resposta.getStatus()).isEqualTo(201);
        return json.readTree(resposta.getContentAsString());
    }

    private JsonNode agir(String id, String acao, int statusEsperado) throws Exception {
        var resposta = mvc().perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"" + acao + "\"}"))
                .andReturn().getResponse();
        assertThat(resposta.getStatus()).as(acao).isEqualTo(statusEsperado);
        return json.readTree(resposta.getContentAsString());
    }

    private JsonNode consultar(String id) throws Exception {
        var resposta = mvc().perform(get("/pedidos/" + id)).andReturn().getResponse();
        assertThat(resposta.getStatus()).isEqualTo(200);
        return json.readTree(resposta.getContentAsString());
    }

    private JsonNode fluxo(String valorProdutos, String frete, String... acoes) throws Exception {
        JsonNode pedido = criar(valorProdutos, frete);
        String id = pedido.get("id").asText();
        for (String acao : acoes) {
            pedido = agir(id, acao, 200);
        }
        return pedido;
    }

    @Test
    void criaPedidoAguardandoPagamento() throws Exception {
        JsonNode pedido = criar("200.00", "20.00");

        assertThat(pedido.get("situacao").asText()).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(pedido.get("descricao").asText()).isEqualTo("Aguardando pagamento");
        assertThat(pedido.get("valorTotal").asText()).isEqualTo("220.00");
        assertThat(pedido.get("valorReembolsado").asText()).isEqualTo("0.00");
        assertThat(pedido.get("estoqueDevolvido").asBoolean()).isFalse();
        assertThat(pedido.get("coletaAgendada").asBoolean()).isFalse();
        assertThat(pedido.get("historico").toString()).isEqualTo("[\"AGUARDANDO_PAGAMENTO\"]");
    }

    @Test
    void exemplo1CancelamentoEmSeparacao() throws Exception {
        JsonNode pedido = fluxo("200.00", "20.00", "PAGAR", "SEPARAR", "CANCELAR");

        assertThat(pedido.get("situacao").asText()).isEqualTo("CANCELADO");
        assertThat(pedido.get("descricao").asText()).isEqualTo("Cancelado");
        assertThat(pedido.get("valorTotal").asText()).isEqualTo("220.00");
        assertThat(pedido.get("valorReembolsado").asText()).isEqualTo("205.00");
        assertThat(pedido.get("estoqueDevolvido").asBoolean()).isTrue();
        assertThat(pedido.get("coletaAgendada").asBoolean()).isFalse();
        assertThat(pedido.get("historico").toString())
                .isEqualTo("[\"AGUARDANDO_PAGAMENTO\",\"PAGO\",\"EM_SEPARACAO\",\"CANCELADO\"]");
    }

    @Test
    void exemplo2Devolucao() throws Exception {
        JsonNode pedido = fluxo("150.00", "12.50", "PAGAR", "SEPARAR", "ENVIAR", "ENTREGAR", "DEVOLVER");

        assertThat(pedido.get("situacao").asText()).isEqualTo("DEVOLVIDO");
        assertThat(pedido.get("descricao").asText()).isEqualTo("Devolvido");
        assertThat(pedido.get("valorTotal").asText()).isEqualTo("162.50");
        assertThat(pedido.get("valorReembolsado").asText()).isEqualTo("150.00");
        assertThat(pedido.get("estoqueDevolvido").asBoolean()).isFalse();
        assertThat(pedido.get("coletaAgendada").asBoolean()).isTrue();
        assertThat(pedido.get("historico").toString()).isEqualTo(
                "[\"AGUARDANDO_PAGAMENTO\",\"PAGO\",\"EM_SEPARACAO\",\"ENVIADO\",\"ENTREGUE\",\"DEVOLVIDO\"]");
    }

    @Test
    void exemplo3CancelamentoAntesDoPagamento() throws Exception {
        JsonNode pedido = fluxo("80.00", "0.00", "CANCELAR");

        assertThat(pedido.get("situacao").asText()).isEqualTo("CANCELADO");
        assertThat(pedido.get("valorReembolsado").asText()).isEqualTo("0.00");
        assertThat(pedido.get("estoqueDevolvido").asBoolean()).isFalse();
        assertThat(pedido.get("coletaAgendada").asBoolean()).isFalse();
    }

    @Test
    void exemplo4CancelamentoDepoisDoPagamento() throws Exception {
        JsonNode pedido = fluxo("250.00", "25.00", "PAGAR", "CANCELAR");

        assertThat(pedido.get("situacao").asText()).isEqualTo("CANCELADO");
        assertThat(pedido.get("valorReembolsado").asText()).isEqualTo("275.00");
        assertThat(pedido.get("estoqueDevolvido").asBoolean()).isFalse();
    }

    @Test
    void exemplo5ReembolsoNuncaFicaNegativo() throws Exception {
        JsonNode pedido = fluxo("10.00", "3.00", "PAGAR", "SEPARAR", "CANCELAR");

        assertThat(pedido.get("situacao").asText()).isEqualTo("CANCELADO");
        assertThat(pedido.get("valorTotal").asText()).isEqualTo("13.00");
        assertThat(pedido.get("valorReembolsado").asText()).isEqualTo("0.00");
        assertThat(pedido.get("estoqueDevolvido").asBoolean()).isTrue();
    }

    @Test
    void exemplo6EnviarSemSepararNaoEPermitido() throws Exception {
        String id = criar("99.90", "15.00").get("id").asText();
        agir(id, "PAGAR", 200);

        assertThat(agir(id, "ENVIAR", 409).get("erro").asText()).isEqualTo("ACAO_NAO_PERMITIDA");

        JsonNode atual = consultar(id);
        assertThat(atual.get("situacao").asText()).isEqualTo("PAGO");
        assertThat(atual.get("historico").toString()).isEqualTo("[\"AGUARDANDO_PAGAMENTO\",\"PAGO\"]");
    }

    @Test
    void exemplo7CancelarDepoisDeEnviadoNaoEPermitido() throws Exception {
        String id = criar("100.00", "10.00").get("id").asText();
        agir(id, "PAGAR", 200);
        agir(id, "SEPARAR", 200);
        agir(id, "ENVIAR", 200);

        assertThat(agir(id, "CANCELAR", 409).get("erro").asText()).isEqualTo("ACAO_NAO_PERMITIDA");

        JsonNode atual = consultar(id);
        assertThat(atual.get("situacao").asText()).isEqualTo("ENVIADO");
        assertThat(atual.get("descricao").asText()).isEqualTo("A caminho");
        assertThat(atual.get("valorReembolsado").asText()).isEqualTo("0.00");
    }

    @Test
    void exemplo8AcaoQueNaoExiste() throws Exception {
        String id = criar("100.00", "10.00").get("id").asText();

        assertThat(agir(id, "TROCAR", 400).get("erro").asText()).isEqualTo("ACAO_INVALIDA");
        assertThat(consultar(id).get("situacao").asText()).isEqualTo("AGUARDANDO_PAGAMENTO");
    }

    @Test
    void acaoNaoInformada() throws Exception {
        String id = criar("100.00", "10.00").get("id").asText();

        var resposta = mvc().perform(post("/pedidos/" + id + "/acoes")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse();

        assertThat(resposta.getStatus()).isEqualTo(400);
        assertThat(json.readTree(resposta.getContentAsString()).get("erro").asText()).isEqualTo("ACAO_INVALIDA");
    }

    @Test
    void pedidoNaoEncontradoVemAntesDaAcaoInvalida() throws Exception {
        var resposta = mvc().perform(post("/pedidos/9999/acoes")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acao\": \"TROCAR\"}"))
                .andReturn().getResponse();

        assertThat(resposta.getStatus()).isEqualTo(404);
        assertThat(json.readTree(resposta.getContentAsString()).get("erro").asText())
                .isEqualTo("PEDIDO_NAO_ENCONTRADO");
    }

    @Test
    void consultaDePedidoInexistente() throws Exception {
        var resposta = mvc().perform(get("/pedidos/9999")).andReturn().getResponse();

        assertThat(resposta.getStatus()).isEqualTo(404);
        assertThat(json.readTree(resposta.getContentAsString()).get("erro").asText())
                .isEqualTo("PEDIDO_NAO_ENCONTRADO");
    }

    @Test
    void criacaoComValoresInvalidos() throws Exception {
        String[] corpos = {
                "{\"valorProdutos\": 0, \"frete\": 10.00}",
                "{\"valorProdutos\": -1.00, \"frete\": 10.00}",
                "{\"frete\": 10.00}",
                "{\"valorProdutos\": 100.00, \"frete\": -0.01}",
                "{\"valorProdutos\": 100.00}"};

        for (String corpo : corpos) {
            var resposta = mvc().perform(post("/pedidos")
                    .contentType(MediaType.APPLICATION_JSON).content(corpo)).andReturn().getResponse();

            assertThat(resposta.getStatus()).as(corpo).isEqualTo(400);
            assertThat(json.readTree(resposta.getContentAsString()).get("erro").asText()).isEqualTo("PEDIDO_INVALIDO");
        }
    }

    @Test
    void pedidoFinalizadoNaoAceitaMaisNada() throws Exception {
        String id = criar("100.00", "10.00").get("id").asText();
        agir(id, "CANCELAR", 200);

        for (String acao : new String[]{"PAGAR", "SEPARAR", "ENVIAR", "ENTREGAR", "CANCELAR", "DEVOLVER"}) {
            assertThat(agir(id, acao, 409).get("erro").asText()).isEqualTo("ACAO_NAO_PERMITIDA");
        }
        assertThat(consultar(id).get("historico").toString())
                .isEqualTo("[\"AGUARDANDO_PAGAMENTO\",\"CANCELADO\"]");
    }

    @Test
    void cadaPedidoTemSeuProprioId() throws Exception {
        assertThat(criar("10.00", "1.00").get("id").asText())
                .isNotEqualTo(criar("10.00", "1.00").get("id").asText());
    }

    @Test
    void valoresSaemComDuasCasasDecimais() throws Exception {
        var resposta = mvc().perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 200, \"frete\": 20}")).andReturn().getResponse();

        assertThat(resposta.getContentAsString())
                .contains("\"valorProdutos\":200.00")
                .contains("\"frete\":20.00")
                .contains("\"valorTotal\":220.00")
                .contains("\"valorReembolsado\":0.00");
    }
}
