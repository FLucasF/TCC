package com.loja.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testResumoSuccess() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
            .andExpect(jsonPath("$.descontoCupom").value(40.97))
            .andExpect(jsonPath("$.frete").value(33.10))
            .andExpect(jsonPath("$.prazoEntregaDias").value(2))
            .andExpect(jsonPath("$.ajustePagamento").value(-20.09))
            .andExpect(jsonPath("$.totalFinal").value(381.74))
            .andExpect(jsonPath("$.parcelas").value(1))
            .andExpect(jsonPath("$.valorParcela").value(381.74));
    }

    @Test
    void testResumoPedidoInvalido() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void testResumoModalidadeInvalida() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void testResumoCupomInvalido() throws Exception {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");

        mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }
}
