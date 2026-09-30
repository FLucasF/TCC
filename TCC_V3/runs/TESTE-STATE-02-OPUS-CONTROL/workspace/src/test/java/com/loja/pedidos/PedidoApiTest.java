package com.loja.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.contains;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** O contrato combinado com o desenvolvedor do site. */
@SpringBootTest
@AutoConfigureMockMvc
class PedidoApiTest {

    @Autowired
    private MockMvc mockMvc;

    private String criar(String corpo) throws Exception {
        String resposta = mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.descricao").value("Aguardando pagamento"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions acao(String id, String nome) throws Exception {
        return mockMvc.perform(post("/pedidos/{id}/acoes", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\":\"" + nome + "\"}"));
    }

    private ResultActions acoes(String id, String... nomes) throws Exception {
        ResultActions ultima = null;
        for (String nome : nomes) {
            ultima = acao(id, nome).andExpect(status().isOk());
        }
        return ultima;
    }

    @Test
    void exemplo1_cancelamentoEmSeparacao() throws Exception {
        String id = criar("{\"valorProdutos\":200.00,\"frete\":20.00}");

        acoes(id, "PAGAR", "SEPARAR", "CANCELAR")
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.descricao").value("Cancelado"))
                .andExpect(jsonPath("$.valorTotal").value(220.00))
                .andExpect(jsonPath("$.valorReembolsado").value(205.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(true))
                .andExpect(jsonPath("$.coletaAgendada").value(false))
                .andExpect(jsonPath("$.historico", contains(
                        "AGUARDANDO_PAGAMENTO", "PAGO", "EM_SEPARACAO", "CANCELADO")));
    }

    @Test
    void exemplo2_devolucaoDepoisDeEntregue() throws Exception {
        String id = criar("{\"valorProdutos\":150.00,\"frete\":12.50}");

        acoes(id, "PAGAR", "SEPARAR", "ENVIAR", "ENTREGAR", "DEVOLVER")
                .andExpect(jsonPath("$.situacao").value("DEVOLVIDO"))
                .andExpect(jsonPath("$.valorTotal").value(162.50))
                .andExpect(jsonPath("$.valorReembolsado").value(150.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(false))
                .andExpect(jsonPath("$.coletaAgendada").value(true));
    }

    @Test
    void exemplo3_cancelamentoAntesDoPagamento() throws Exception {
        String id = criar("{\"valorProdutos\":80.00,\"frete\":0.00}");

        acoes(id, "CANCELAR")
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.valorReembolsado").value(0.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(false))
                .andExpect(jsonPath("$.coletaAgendada").value(false));
    }

    @Test
    void exemplo4_cancelamentoDepoisDePago() throws Exception {
        String id = criar("{\"valorProdutos\":250.00,\"frete\":25.00}");

        acoes(id, "PAGAR", "CANCELAR")
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.valorReembolsado").value(275.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(false));
    }

    @Test
    void exemplo5_taxaMaiorQueOTotal() throws Exception {
        String id = criar("{\"valorProdutos\":10.00,\"frete\":3.00}");

        acoes(id, "PAGAR", "SEPARAR", "CANCELAR")
                .andExpect(jsonPath("$.situacao").value("CANCELADO"))
                .andExpect(jsonPath("$.valorTotal").value(13.00))
                .andExpect(jsonPath("$.valorReembolsado").value(0.00))
                .andExpect(jsonPath("$.estoqueDevolvido").value(true));
    }

    @Test
    void exemplo6_enviarSemSepararNaoVale() throws Exception {
        String id = criar("{\"valorProdutos\":99.90,\"frete\":15.00}");
        acoes(id, "PAGAR");

        acao(id, "ENVIAR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("ACAO_NAO_PERMITIDA"));

        mockMvc.perform(get("/pedidos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PAGO"))
                .andExpect(jsonPath("$.descricao").value("Pagamento confirmado"))
                .andExpect(jsonPath("$.historico", contains("AGUARDANDO_PAGAMENTO", "PAGO")));
    }

    @Test
    void exemplo7_cancelarPedidoEnviadoNaoVale() throws Exception {
        String id = criar("{\"valorProdutos\":100.00,\"frete\":10.00}");
        acoes(id, "PAGAR", "SEPARAR", "ENVIAR");

        acao(id, "CANCELAR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("ACAO_NAO_PERMITIDA"));

        mockMvc.perform(get("/pedidos/{id}", id))
                .andExpect(jsonPath("$.situacao").value("ENVIADO"))
                .andExpect(jsonPath("$.valorReembolsado").value(0.00));
    }

    @Test
    void exemplo8_acaoQueNaoExiste() throws Exception {
        String id = criar("{\"valorProdutos\":100.00,\"frete\":10.00}");

        acao(id, "TROCAR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("ACAO_INVALIDA"));

        mockMvc.perform(get("/pedidos/{id}", id))
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    void acaoAusenteEInvalida() throws Exception {
        String id = criar("{\"valorProdutos\":100.00,\"frete\":10.00}");

        mockMvc.perform(post("/pedidos/{id}/acoes", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("ACAO_INVALIDA"));

        mockMvc.perform(post("/pedidos/{id}/acoes", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("ACAO_INVALIDA"));
    }

    @Test
    void pedidoInexistenteVemAntesDaAcaoInvalida() throws Exception {
        mockMvc.perform(get("/pedidos/{id}", "nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("PEDIDO_NAO_ENCONTRADO"));

        acao("nao-existe", "TROCAR")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("PEDIDO_NAO_ENCONTRADO"));
    }

    @Test
    void valoresInvalidosNaCriacao() throws Exception {
        for (String corpo : new String[] {
                "{\"valorProdutos\":0,\"frete\":10.00}",
                "{\"valorProdutos\":-1.00,\"frete\":10.00}",
                "{\"frete\":10.00}",
                "{\"valorProdutos\":100.00,\"frete\":-0.01}",
                "{\"valorProdutos\":100.00}",
                "{}" }) {
            mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
        }
    }

    @Test
    void cadaPedidoTemUmIdDiferente() throws Exception {
        String primeiro = criar("{\"valorProdutos\":10.00,\"frete\":1.00}");
        String segundo = criar("{\"valorProdutos\":10.00,\"frete\":1.00}");

        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    void valoresSaemComDuasCasasDecimais() throws Exception {
        String corpo = mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorProdutos\":200,\"frete\":20}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(corpo)
                .contains("\"valorProdutos\":200.00")
                .contains("\"frete\":20.00")
                .contains("\"valorTotal\":220.00")
                .contains("\"valorReembolsado\":0.00");
    }
}
