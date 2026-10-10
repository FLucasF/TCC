package com.loja.checkout.controller;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testResumoEndpoint() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Camiseta", 79.90, 2, 0.30),
                new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(10.24))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47));
    }

    @Test
    void testResumoEndpointWithError() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new Item("Produto barato", 50.00, 1, 0.10)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }
}
