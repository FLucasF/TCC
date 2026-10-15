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

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String EXEMPLO_1 = """
            {
              "itens": [
                { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
              ],
              "modalidadeEntrega": "EXPRESSA",
              "cupom": "BEMVINDO10",
              "formaPagamento": "PIX",
              "parcelas": 1
            }
            """;

    @Test
    void responde_o_resumo_do_exemplo_1() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON).content(EXEMPLO_1))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 40.97,
                          "frete": 33.10,
                          "prazoEntregaDias": 2,
                          "ajustePagamento": -20.09,
                          "totalFinal": 381.74,
                          "parcelas": 1,
                          "valorParcela": 381.74
                        }
                        """));
    }

    @Test
    void cupom_ausente_no_json_vale_como_sem_cupom() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [
                            { "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }
                          ],
                          "modalidadeEntrega": "RETIRADA_LOJA",
                          "formaPagamento": "PIX"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.totalFinal").value(18.90));
    }

    @Test
    void erro_de_negocio_sai_com_400_e_codigo() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [],
                          "modalidadeEntrega": "EXPRESSA",
                          "formaPagamento": "PIX"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}"));
    }

    @Test
    void corpo_ilegivel_sai_como_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON).content("nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}"));
    }
}
