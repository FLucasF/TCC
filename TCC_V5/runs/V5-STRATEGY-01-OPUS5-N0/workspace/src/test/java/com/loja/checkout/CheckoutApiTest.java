package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /checkout/resumo devolve o resumo do exemplo do anexo")
    void resumoDoAnexo() throws Exception {
        String pedido = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-18.64))
                .andExpect(jsonPath("$.totalFinal").value(354.19))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(354.19))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Os valores em dinheiro saem com 2 casas decimais")
    void valoresComDuasCasas() throws Exception {
        String pedido = """
                {
                  "itens": [{"nome": "Camiseta", "precoUnitario": 100, "quantidade": 1, "pesoKg": 1}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":101.00")));
    }

    @Test
    @DisplayName("Pedido recusado devolve so o codigo do problema")
    void pedidoRecusado() throws Exception {
        String pedido = """
                {
                  "itens": [{"nome": "Caixa", "precoUnitario": 10.00, "quantidade": 6, "pesoKg": 1.00}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    @DisplayName("Cupom ausente no JSON e tratado como compra sem cupom")
    void semCupom() throws Exception {
        String pedido = """
                {
                  "itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(429.29));
    }

    @Test
    @DisplayName("JSON ilegivel e recusado como pedido invalido")
    void jsonIlegivel() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    @DisplayName("Corpo vazio e recusado como pedido invalido")
    void corpoVazio() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }
}
