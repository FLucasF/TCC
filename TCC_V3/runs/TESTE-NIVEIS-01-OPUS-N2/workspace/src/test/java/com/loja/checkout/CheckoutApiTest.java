package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** O contrato que o site usa: POST /checkout/resumo. */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    @Autowired
    private MockMvc site;

    @Test
    void devolve_o_resumo_da_compra() throws Exception {
        site.perform(post("/checkout/resumo")
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
                        """, true));
    }

    @Test
    void devolve_os_valores_com_duas_casas_decimais() throws Exception {
        site.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Meia", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.1}],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":10.00")));
    }

    @Test
    void devolve_so_o_codigo_do_problema_quando_recusa() throws Exception {
        site.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Mala", "precoUnitario": 250.00, "quantidade": 1, "pesoKg": 9.0}],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"MODALIDADE_INDISPONIVEL\"}", true))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void recusa_corpo_vazio() throws Exception {
        site.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }
}
