package com.loja.checkout.api;

import org.junit.jupiter.api.Test;
import com.loja.checkout.regra.CalculadoraResumo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumoController.class)
@Import(CalculadoraResumo.class)
class ResumoControllerTest {

    private static final String PEDIDO_EXEMPLO_1 = """
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
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void devolveOResumoDaCompra() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO_EXEMPLO_1))
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
                .andExpect(jsonPath("$.brinde").value(false))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":391.47")));
    }

    @Test
    void devolveSoOCodigoDoErro() throws Exception {
        String pedidoSemCarrinho = PEDIDO_EXEMPLO_1.replaceAll("\"itens\": \\[[\\s\\S]*?\\],", "\"itens\": [],");

        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoSemCarrinho))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    void jsonMalformadoDevolvePedidoInvalido() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": \"nao é lista\""))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }
}
