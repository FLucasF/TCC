package com.loja.resumo.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loja.resumo.calculo.ResumoCompraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResumoCompraController.class)
@Import({ResumoCompraService.class, TratadorDeErros.class})
class ResumoCompraControllerTest {

    @Autowired
    private MockMvc mvc;

    private static final String EXEMPLO_1 = """
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

    @Test
    void respondeResumoComDuasCasasDecimais() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType("application/json").content(EXEMPLO_1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":409.70")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"descontoCupom\":40.97")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"ajustePagamento\":-20.60")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":391.47")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"brinde\":false")));
    }

    @Test
    void recusaPedidoComCodigoDoErro() throws Exception {
        String semRegiao = EXEMPLO_1.replace("\"regiao\": \"NORTE\"", "\"regiao\": \"MARTE\"");
        mvc.perform(post("/checkout/resumo").contentType("application/json").content(semRegiao))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"REGIAO_INVALIDA\"}"));
    }
}
