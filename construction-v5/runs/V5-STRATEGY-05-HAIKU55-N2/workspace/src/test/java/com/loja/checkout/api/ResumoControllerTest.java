package com.loja.checkout.api;

import com.loja.checkout.calculo.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumoController.class)
@Import(CalculadoraResumo.class)
class ResumoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void devolveResumoComDuasCasasDecimais() throws Exception {
        String pedido = """
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

        mvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(pedido))
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
    void recusaPedidoComCodigoDeErro() throws Exception {
        String pedido = """
                {
                  "itens": [{"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 6.00}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"MODALIDADE_INDISPONIVEL\"}"));
    }
}
