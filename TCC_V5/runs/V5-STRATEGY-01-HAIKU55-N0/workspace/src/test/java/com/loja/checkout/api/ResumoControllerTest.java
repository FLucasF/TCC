package com.loja.checkout.api;

import com.loja.checkout.calculo.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    void devolveResumoComValoresComDuasCasas() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO_EXEMPLO_1))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"subtotalProdutos\":409.70")))
                .andExpect(content().string(containsString("\"descontoCupom\":40.97")))
                .andExpect(content().string(containsString("\"frete\":33.10")))
                .andExpect(content().string(containsString("\"prazoEntregaDias\":2")))
                .andExpect(content().string(containsString("\"seguro\":10.24")))
                .andExpect(content().string(containsString("\"ajustePagamento\":-20.60")))
                .andExpect(content().string(containsString("\"totalFinal\":391.47")))
                .andExpect(content().string(containsString("\"parcelas\":1")))
                .andExpect(content().string(containsString("\"valorParcela\":391.47")))
                .andExpect(content().string(containsString("\"creditoProximaCompra\":0.00")))
                .andExpect(content().string(containsString("\"brinde\":false")));
    }

    @Test
    void cupomInvalidoDevolveSoOCodigoDoErro() throws Exception {
        String pedido = PEDIDO_EXEMPLO_1.replace("BEMVINDO10", "NAOEXISTE");

        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"CUPOM_INVALIDO\"}", true));
    }
}
