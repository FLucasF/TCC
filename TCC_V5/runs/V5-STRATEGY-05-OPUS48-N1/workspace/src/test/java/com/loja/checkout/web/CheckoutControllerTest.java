package com.loja.checkout.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void resumoDoExemploDoAnexo() throws Exception {
        String corpo = """
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
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
    void duasCasasDecimaisNoCorpo() throws Exception {
        String corpo = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")));
    }

    @Test
    void pedidoRecusadoDevolveSoOCodigo() throws Exception {
        String corpo = """
                {
                  "itens": [],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"))
                .andExpect(jsonPath("$.totalFinal").doesNotExist());
    }
}
