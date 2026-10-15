package com.loja.pedidos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.pedidos.dto.AcaoRequest;
import com.loja.pedidos.dto.CriarPedidoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

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
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setValorProdutos(new BigDecimal("200.00"));
        request.setFrete(new BigDecimal("20.00"));

        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"))
            .andExpect(jsonPath("$.descricao").value("Aguardando pagamento"))
            .andExpect(jsonPath("$.valorProdutos").value(200.00))
            .andExpect(jsonPath("$.frete").value(20.00))
            .andExpect(jsonPath("$.valorTotal").value(220.00))
            .andExpect(jsonPath("$.valorReembolsado").value(0.00))
            .andExpect(jsonPath("$.estoqueDevolvido").value(false))
            .andExpect(jsonPath("$.coletaAgendada").value(false))
            .andExpect(jsonPath("$.historico", hasSize(1)))
            .andExpect(jsonPath("$.historico[0]").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    void criarPedidoComValorProdutosZero() throws Exception {
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setValorProdutos(BigDecimal.ZERO);
        request.setFrete(new BigDecimal("20.00"));

        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void criarPedidoComValorProdutosNegativo() throws Exception {
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setValorProdutos(new BigDecimal("-100.00"));
        request.setFrete(new BigDecimal("20.00"));

        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void criarPedidoComFreteNegativo() throws Exception {
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setValorProdutos(new BigDecimal("200.00"));
        request.setFrete(new BigDecimal("-20.00"));

        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void criarPedidoSemValorProdutos() throws Exception {
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setFrete(new BigDecimal("20.00"));

        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void criarPedidoSemFrete() throws Exception {
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setValorProdutos(new BigDecimal("200.00"));

        mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void obterPedidoNaoExistente() throws Exception {
        mockMvc.perform(get("/pedidos/inexistente"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("PEDIDO_NAO_ENCONTRADO"));
    }

    @Test
    void executarAcaoNaoExistente() throws Exception {
        CriarPedidoRequest criarRequest = new CriarPedidoRequest();
        criarRequest.setValorProdutos(new BigDecimal("200.00"));
        criarRequest.setFrete(new BigDecimal("20.00"));

        String response = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criarRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        AcaoRequest acaoRequest = new AcaoRequest();
        acaoRequest.setAcao("TROCAR");

        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acaoRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("ACAO_INVALIDA"));
    }

    @Test
    void executarAcaoNaoPermitida() throws Exception {
        CriarPedidoRequest criarRequest = new CriarPedidoRequest();
        criarRequest.setValorProdutos(new BigDecimal("200.00"));
        criarRequest.setFrete(new BigDecimal("20.00"));

        String response = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criarRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        AcaoRequest pagarRequest = new AcaoRequest();
        pagarRequest.setAcao("PAGAR");
        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagarRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("PAGO"));

        AcaoRequest enviarRequest = new AcaoRequest();
        enviarRequest.setAcao("ENVIAR");
        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enviarRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.erro").value("ACAO_NAO_PERMITIDA"));
    }

    @Test
    void executarAcaoSemBody() throws Exception {
        CriarPedidoRequest criarRequest = new CriarPedidoRequest();
        criarRequest.setValorProdutos(new BigDecimal("200.00"));
        criarRequest.setFrete(new BigDecimal("20.00"));

        String response = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criarRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("ACAO_INVALIDA"));
    }

    @Test
    void fluxoCompletoComSucesso() throws Exception {
        CriarPedidoRequest criarRequest = new CriarPedidoRequest();
        criarRequest.setValorProdutos(new BigDecimal("200.00"));
        criarRequest.setFrete(new BigDecimal("20.00"));

        String response = mockMvc.perform(post("/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criarRequest)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        AcaoRequest pagarRequest = new AcaoRequest();
        pagarRequest.setAcao("PAGAR");
        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagarRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("PAGO"))
            .andExpect(jsonPath("$.descricao").value("Pagamento confirmado"))
            .andExpect(jsonPath("$.historico", hasSize(2)));

        AcaoRequest separarRequest = new AcaoRequest();
        separarRequest.setAcao("SEPARAR");
        mockMvc.perform(post("/pedidos/" + id + "/acoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(separarRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("EM_SEPARACAO"))
            .andExpect(jsonPath("$.descricao").value("Separando seus produtos"))
            .andExpect(jsonPath("$.historico", hasSize(3)));

        mockMvc.perform(get("/pedidos/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.situacao").value("EM_SEPARACAO"))
            .andExpect(jsonPath("$.historico", hasSize(3)));
    }
}
