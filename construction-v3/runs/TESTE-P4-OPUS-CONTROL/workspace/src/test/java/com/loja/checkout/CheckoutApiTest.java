package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resumo_devolve_o_contrato_combinado() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                                    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                                  ],
                                  "modalidadeEntrega": "EXPRESSA",
                                  "cupom": null,
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
                          "descontoCupom": 0.00,
                          "frete": 0.00,
                          "prazoEntregaDias": 2,
                          "imposto": 49.16,
                          "ajustePagamento": -22.94,
                          "totalFinal": 435.92,
                          "parcelas": 1,
                          "valorParcela": 435.92,
                          "creditoProximaCompra": 20.48,
                          "brinde": false
                        }
                        """, true));
    }

    @Test
    void campos_opcionais_podem_nem_vir() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    { "nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25 }
                                  ],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "BOLETO",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(469.27));
    }

    @Test
    void valores_em_dinheiro_saem_com_duas_casas() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{ "nome": "Meia", "precoUnitario": 20.00, "quantidade": 1, "pesoKg": 0.10 }],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "formaPagamento": "CARTAO",
                                  "nivelClube": "PRATA",
                                  "regiao": "NORTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":21.40")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.40")));
    }

    @Test
    void erro_de_negocio_vira_400_com_o_codigo() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{ "nome": "Bota", "precoUnitario": 100.00, "quantidade": 3, "pesoKg": 2.50 }],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void corpo_vazio_vira_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }
}
