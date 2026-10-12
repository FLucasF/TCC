package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResumoController.class)
@Import(ResumoService.class)
class ResumoControllerTest {

    private static final String EXEMPLO_UM = """
            {
              "itens": [
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
              ],
              "modalidadeEntrega": "EXPRESSA",
              "cupom": "BEMVINDO10",
              "formaPagamento": "PIX",
              "parcelas": 1,
              "nivelClube": "BRONZE",
              "regiao": "NORTE"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void devolveOResumoNoFormatoDoSite() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EXEMPLO_UM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void devolveErroComoCodigo() throws Exception {
        String semItens = """
                {"itens": [], "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX", "nivelClube": "BRONZE", "regiao": "NORTE"}
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(semItens))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
