package com.loja.checkout.web;

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
    private MockMvc mvc;

    @Test
    void sucessoDevolveResumoComDuasCasas() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                    {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                  ],
                  "modalidadeEntrega":"EXPRESSA",
                  "cupom":"BEMVINDO10",
                  "formaPagamento":"PIX",
                  "parcelas":1,
                  "nivelClube":"BRONZE",
                  "regiao":"NORTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":409.70")))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void erroDevolveSoOCodigo() throws Exception {
        String corpo = """
                {
                  "itens": [],
                  "modalidadeEntrega":"EXPRESSA",
                  "formaPagamento":"PIX",
                  "nivelClube":"BRONZE",
                  "regiao":"NORTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
