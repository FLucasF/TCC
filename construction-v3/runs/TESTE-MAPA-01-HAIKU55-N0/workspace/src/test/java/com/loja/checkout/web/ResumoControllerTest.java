package com.loja.checkout.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void respondeOResumoDoExemplo1() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content("""
                                {
                                  "itens": [
                                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                                    {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                                  ],
                                  "modalidadeEntrega": "EXPRESSA",
                                  "cupom": "BEMVINDO10",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "NORTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 40.97,
                          "frete": 33.10,
                          "prazoEntregaDias": 2,
                          "seguro": 10.24,
                          "ajustePagamento": -20.60,
                          "totalFinal": 391.47,
                          "parcelas": 1,
                          "valorParcela": 391.47,
                          "creditoProximaCompra": 0.00,
                          "brinde": false
                        }
                        """));
    }

    @Test
    void recusaComCodigoDeErroQuandoModalidadeNaoExiste() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content("""
                                {
                                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                                  "modalidadeEntrega": "DRONE",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"MODALIDADE_INVALIDA\"}"));
    }

    @Test
    void recusaCorpoMalformado() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}"));
    }
}
