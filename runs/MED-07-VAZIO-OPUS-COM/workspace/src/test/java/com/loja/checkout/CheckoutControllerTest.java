package com.loja.checkout;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolve_o_resumo_no_formato_combinado() throws Exception {
        String corpo = """
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
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
    void valores_saem_sempre_com_duas_casas() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 20, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"totalFinal\":20.00")))
                .andExpect(content().string(containsString("\"frete\":0.00")));
    }

    @Test
    void devolve_o_codigo_do_erro_com_400() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void corpo_ilegivel_vira_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
