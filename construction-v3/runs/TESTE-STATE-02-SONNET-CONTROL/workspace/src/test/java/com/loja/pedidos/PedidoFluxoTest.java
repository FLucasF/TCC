package com.loja.pedidos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PedidoFluxoTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private String extrairCampo(String json, String campo) {
        Matcher matcher = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"?([^,\"}]+)\"?").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Campo " + campo + " não encontrado em: " + json);
        }
        return matcher.group(1);
    }

    private String criar(MockMvc mvc, double valorProdutos, double frete) throws Exception {
        MvcResult resultado = mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorProdutos\": " + valorProdutos + ", \"frete\": " + frete + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return extrairCampo(resultado.getResponse().getContentAsString(), "id");
    }

    private MvcResult agir(MockMvc mvc, String id, String acao) throws Exception {
        return mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"" + acao + "\"}"))
                .andReturn();
    }

    @Test
    void exemplo1_cancelarEmSeparacaoAplicaTaxa() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 200.00, 20.00);

        agir(mvc, id, "PAGAR");
        agir(mvc, id, "SEPARAR");

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"CANCELAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.valorTotal").value(220.00))
                .andExpect(jsonPath("$.valorReembolsado").value(205.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(true))
                .andExpect(jsonPath("$.coletaAgendada").value(false));
    }

    @Test
    void exemplo2_devolucaoAposEntrega() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 150.00, 12.50);

        agir(mvc, id, "PAGAR");
        agir(mvc, id, "SEPARAR");
        agir(mvc, id, "ENVIAR");
        agir(mvc, id, "ENTREGAR");

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"DEVOLVER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("DEVOLVIDO"))
                .andExpect(jsonPath("$.valorTotal").value(162.50))
                .andExpect(jsonPath("$.valorReembolsado").value(150.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(false))
                .andExpect(jsonPath("$.coletaAgendada").value(true));
    }

    @Test
    void exemplo3_cancelarAguardandoPagamentoSemReembolso() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 80.00, 0.00);

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"CANCELAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.valorReembolsado").value(0.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(false))
                .andExpect(jsonPath("$.coletaAgendada").value(false));
    }

    @Test
    void exemplo4_cancelarPagoReembolsaTotal() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 250.00, 25.00);

        agir(mvc, id, "PAGAR");

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"CANCELAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.valorReembolsado").value(275.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(false));
    }

    @Test
    void exemplo5_taxaMaiorQueTotalNaoFicaNegativa() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 10.00, 3.00);

        agir(mvc, id, "PAGAR");
        agir(mvc, id, "SEPARAR");

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"CANCELAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.valorTotal").value(13.00))
                .andExpect(jsonPath("$.valorReembolsado").value(0.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(true));
    }

    @Test
    void exemplo6_enviarSemSepararDaErro() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 99.90, 15.00);

        agir(mvc, id, "PAGAR");

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"ENVIAR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("ACAO_NAO_PERMITIDA"));

        mvc.perform(get("/pedidos/" + id))
                .andExpect(jsonPath("$.situacao").value("PAGO"));
    }

    @Test
    void exemplo7_cancelarEnviadoDaErro() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 100.00, 10.00);

        agir(mvc, id, "PAGAR");
        agir(mvc, id, "SEPARAR");
        agir(mvc, id, "ENVIAR");

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"CANCELAR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("ACAO_NAO_PERMITIDA"));

        mvc.perform(get("/pedidos/" + id))
                .andExpect(jsonPath("$.situacao").value("ENVIADO"));
    }

    @Test
    void exemplo8_acaoInexistente() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 50.00, 5.00);

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"TROCAR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("ACAO_INVALIDA"));
    }

    @Test
    void pedidoInvalidoAoCriar() throws Exception {
        MockMvc mvc = mockMvc();
        mvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorProdutos\": 0, \"frete\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void pedidoNaoEncontrado() throws Exception {
        MockMvc mvc = mockMvc();
        mvc.perform(get("/pedidos/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("PEDIDO_NAO_ENCONTRADO"));
    }

    @Test
    void historicoRegistraTodasAsSituacoes() throws Exception {
        MockMvc mvc = mockMvc();
        String id = criar(mvc, 200.00, 20.00);

        agir(mvc, id, "PAGAR");

        mvc.perform(post("/pedidos/" + id + "/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\": \"SEPARAR\"}"))
                .andExpect(jsonPath("$.historico.length()").value(3))
                .andExpect(jsonPath("$.historico[0]").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.historico[1]").value("PAGO"))
                .andExpect(jsonPath("$.historico[2]").value("EM_SEPARACAO"));
    }
}
