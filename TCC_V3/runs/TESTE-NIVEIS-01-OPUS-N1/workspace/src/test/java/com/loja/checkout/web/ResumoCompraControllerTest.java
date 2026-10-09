package com.loja.checkout.web;

import org.junit.jupiter.api.DisplayName;
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
class ResumoCompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Resumo da compra do anexo, com todos os valores em duas casas")
    void resumoDaCompra() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 40.97,
                          "frete": 0.00,
                          "prazoEntregaDias": 2,
                          "seguro": 4.10,
                          "ajustePagamento": -18.64,
                          "totalFinal": 354.19,
                          "parcelas": 1,
                          "valorParcela": 354.19,
                          "creditoProximaCompra": 20.48,
                          "brinde": false
                        }
                        """, true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")));
    }

    @Test
    @DisplayName("Pedido sem os campos obrigatorios e recusado com o codigo do problema")
    void pedidoRecusado() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    @DisplayName("Dados da compra ilegiveis tambem sao pedido invalido")
    void corpoIlegivel() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": [{\"precoUnitario\": \"muito\"}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
