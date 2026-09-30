package com.loja.pedidos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void criarPedidoComSucesso() throws Exception {
        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 200.00, \"frete\": 20.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.situacao", is("AGUARDANDO_PAGAMENTO")))
                .andExpect(jsonPath("$.descricao", is("Aguardando pagamento")))
                .andExpect(jsonPath("$.valorProdutos", is(200.00)))
                .andExpect(jsonPath("$.frete", is(20.00)))
                .andExpect(jsonPath("$.valorTotal", is(220.00)))
                .andExpect(jsonPath("$.valorReembolsado", is(0.00)))
                .andExpect(jsonPath("$.estoqueDevolvido", is(false)))
                .andExpect(jsonPath("$.coletaAgendada", is(false)))
                .andExpect(jsonPath("$.historico", hasSize(1)));
    }

    @Test
    void criarPedidoComValorZero() throws Exception {
        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 0, \"frete\": 20.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void criarPedidoComFreteNegativo() throws Exception {
        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 200.00, \"frete\": -10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void criarPedidoSemValorProdutos() throws Exception {
        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"frete\": 20.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void obterPedidoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/pedidos/id-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro", is("PEDIDO_NAO_ENCONTRADO")));
    }

    @Test
    void executarAcaoComSucesso() throws Exception {
        var resultado = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 200.00, \"frete\": 20.00}"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resultado).get("id").asText();

        mockMvc.perform(post("/pedidos/{id}/acoes", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\": \"PAGAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao", is("PAGO")));
    }

    @Test
    void executarAcaoNaoPermitida() throws Exception {
        var resultado = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 200.00, \"frete\": 20.00}"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resultado).get("id").asText();

        mockMvc.perform(post("/pedidos/{id}/acoes", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\": \"ENVIAR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro", is("ACAO_NAO_PERMITIDA")));
    }

    @Test
    void executarAcaoInvalida() throws Exception {
        var resultado = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 200.00, \"frete\": 20.00}"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resultado).get("id").asText();

        mockMvc.perform(post("/pedidos/{id}/acoes", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\": \"TROCAR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("ACAO_INVALIDA")));
    }

    @Test
    void executarAcaoSemAcao() throws Exception {
        var resultado = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorProdutos\": 200.00, \"frete\": 20.00}"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(resultado).get("id").asText();

        mockMvc.perform(post("/pedidos/{id}/acoes", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("ACAO_INVALIDA")));
    }

    @Test
    void executarAcaoEmPedidoNaoEncontrado() throws Exception {
        mockMvc.perform(post("/pedidos/id-inexistente/acoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"acao\": \"PAGAR\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro", is("PEDIDO_NAO_ENCONTRADO")));
    }
}
