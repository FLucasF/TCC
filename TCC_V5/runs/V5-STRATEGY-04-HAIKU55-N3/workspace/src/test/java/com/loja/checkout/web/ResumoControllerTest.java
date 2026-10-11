package com.loja.checkout.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ResumoControllerTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ResumoController(new CalculadoraResumo()))
            .setControllerAdvice(new TratadorErros())
            .build();

    @Test
    void respondeResumoEmJson() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
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
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void devolveSomenteOCodigoDeErro() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                                  "modalidadeEntrega": "EXPRESSA",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "DIAMANTE",
                                  "regiao": "SUL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }
}
