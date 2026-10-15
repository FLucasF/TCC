package com.loja.checkout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.checkout.CheckoutApplication;
import com.loja.checkout.dto.Item;
import com.loja.checkout.dto.RequisicaoCheckout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CheckoutApplication.class)
@AutoConfigureMockMvc
class CheckoutControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exemplo1_ReturnsCorrectSummary() throws Exception {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        );
        requisicao.modalidadeEntrega = "EXPRESSA";
        requisicao.cupom = "BEMVINDO10";
        requisicao.formaPagamento = "PIX";
        requisicao.parcelas = 1;

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requisicao)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotalProdutos", is(409.70)))
            .andExpect(jsonPath("$.descontoCupom", is(40.97)))
            .andExpect(jsonPath("$.frete", is(33.10)))
            .andExpect(jsonPath("$.prazoEntregaDias", is(2)))
            .andExpect(jsonPath("$.ajustePagamento", is(-20.09)))
            .andExpect(jsonPath("$.totalFinal", is(381.74)))
            .andExpect(jsonPath("$.parcelas", is(1)))
            .andExpect(jsonPath("$.valorParcela", is(381.74)));
    }

    @Test
    void pedidoInvalido_ReturnsBadRequest() throws Exception {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = Collections.emptyList();
        requisicao.modalidadeEntrega = "RETIRADA_LOJA";
        requisicao.formaPagamento = "PIX";
        requisicao.parcelas = 1;

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requisicao)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void modalidadeInvalida_ReturnsBadRequest() throws Exception {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = Collections.singletonList(
            new Item("Produto", 100.0, 1, 0.5)
        );
        requisicao.modalidadeEntrega = "INVALIDA";
        requisicao.formaPagamento = "PIX";
        requisicao.parcelas = 1;

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requisicao)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", is("MODALIDADE_INVALIDA")));
    }

    @Test
    void cupomInvalido_ReturnsBadRequest() throws Exception {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = Collections.singletonList(
            new Item("Produto", 100.0, 1, 0.5)
        );
        requisicao.modalidadeEntrega = "RETIRADA_LOJA";
        requisicao.cupom = "CUPOMINVALIDO";
        requisicao.formaPagamento = "PIX";
        requisicao.parcelas = 1;

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requisicao)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro", is("CUPOM_INVALIDO")));
    }
}
