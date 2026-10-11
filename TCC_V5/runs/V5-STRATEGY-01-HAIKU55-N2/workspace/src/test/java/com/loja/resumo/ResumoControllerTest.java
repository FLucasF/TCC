package com.loja.resumo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    private static final String PEDIDO_EXEMPLO1 = """
            {
              "itens": [
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
              ],
              "modalidadeEntrega": "EXPRESSA",
              "cupom": "BEMVINDO10",
              "formaPagamento": "PIX",
              "nivelClube": "BRONZE",
              "regiao": "NORTE"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void devolveResumoNoFormatoEsperado() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(PEDIDO_EXEMPLO1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(10.24))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(391.47))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void erroDevolveSoOCodigo() throws Exception {
        var pedidoSemNivel = PEDIDO_EXEMPLO1.replace("\"nivelClube\": \"BRONZE\"", "\"nivelClube\": \"X\"");
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedidoSemNivel))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }
}
