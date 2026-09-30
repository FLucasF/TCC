package com.loja.pedidos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCriarPedido() throws Exception {
        String requestBody = "{\"valorProdutos\": 200.00, \"frete\": 20.00}";

        mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.situacao", equalTo("AGUARDANDO_PAGAMENTO")))
            .andExpect(jsonPath("$.descricao", equalTo("Aguardando pagamento")))
            .andExpect(jsonPath("$.valorProdutos", equalTo(200.00)))
            .andExpect(jsonPath("$.frete", equalTo(20.00)))
            .andExpect(jsonPath("$.valorTotal", equalTo(220.00)))
            .andExpect(jsonPath("$.valorReembolsado", equalTo(0.00)))
            .andExpect(jsonPath("$.estoqueDevolvido", equalTo(false)))
            .andExpect(jsonPath("$.coletaAgendada", equalTo(false)))
            .andExpect(jsonPath("$.historico", hasSize(1)))
            .andExpect(jsonPath("$.historico[0]", equalTo("AGUARDANDO_PAGAMENTO")));
    }

    @Test
    void testCriarPedidoValorProdutosZero() throws Exception {
        String requestBody = "{\"valorProdutos\": 0, \"frete\": 20.00}";

        mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", equalTo("PEDIDO_INVALIDO")));
    }

    @Test
    void testCriarPedidoValorProdutosNulo() throws Exception {
        String requestBody = "{\"frete\": 20.00}";

        mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", equalTo("PEDIDO_INVALIDO")));
    }

    @Test
    void testCriarPedidoFreteNegativo() throws Exception {
        String requestBody = "{\"valorProdutos\": 200.00, \"frete\": -10.00}";

        mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", equalTo("PEDIDO_INVALIDO")));
    }

    @Test
    void testCriarPedidoFreteNulo() throws Exception {
        String requestBody = "{\"valorProdutos\": 200.00}";

        mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", equalTo("PEDIDO_INVALIDO")));
    }

    @Test
    void testObterPedido() throws Exception {
        String criarRequest = "{\"valorProdutos\": 200.00, \"frete\": 20.00}";

        var response = mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(criarRequest))
            .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        String id = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6,
                                          responseBody.indexOf("\"id\":\"") + 42);

        mockMvc.perform(get("/pedidos/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", equalTo(id)))
            .andExpect(jsonPath("$.situacao", equalTo("AGUARDANDO_PAGAMENTO")));
    }

    @Test
    void testObterPedidoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/pedidos/id-inexistente"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro", equalTo("PEDIDO_NAO_ENCONTRADO")));
    }

    @Test
    void testExecutarAcao() throws Exception {
        String criarRequest = "{\"valorProdutos\": 200.00, \"frete\": 20.00}";

        var response = mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(criarRequest))
            .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        String id = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6,
                                          responseBody.indexOf("\"id\":\"") + 42);

        String acaoRequest = "{\"acao\": \"PAGAR\"}";

        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType("application/json")
                .content(acaoRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao", equalTo("PAGO")))
            .andExpect(jsonPath("$.descricao", equalTo("Pagamento confirmado")));
    }

    @Test
    void testExecutarAcaoInvalida() throws Exception {
        String criarRequest = "{\"valorProdutos\": 200.00, \"frete\": 20.00}";

        var response = mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(criarRequest))
            .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        String id = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6,
                                          responseBody.indexOf("\"id\":\"") + 42);

        String acaoRequest = "{\"acao\": \"TROCAR\"}";

        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType("application/json")
                .content(acaoRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", equalTo("ACAO_INVALIDA")));
    }

    @Test
    void testExecutarAcaoNaoPermitida() throws Exception {
        String criarRequest = "{\"valorProdutos\": 99.90, \"frete\": 15.00}";

        var response = mockMvc.perform(post("/pedidos")
                .contentType("application/json")
                .content(criarRequest))
            .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        String id = responseBody.substring(responseBody.indexOf("\"id\":\"") + 6,
                                          responseBody.indexOf("\"id\":\"") + 42);

        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType("application/json")
                .content("{\"acao\": \"PAGAR\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType("application/json")
                .content("{\"acao\": \"ENVIAR\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro", equalTo("ACAO_NAO_PERMITIDA")));
    }
}
