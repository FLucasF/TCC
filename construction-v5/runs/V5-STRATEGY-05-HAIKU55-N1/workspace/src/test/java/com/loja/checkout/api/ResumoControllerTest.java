package com.loja.checkout.api;

import com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
              "parcelas": 1,
              "nivelClube": "BRONZE",
              "regiao": "NORTE"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void devolveResumoNoFormatoDoContrato() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType("application/json").content(PEDIDO_EXEMPLO_1))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 40.97,
                          "frete": 33.10,
                          "prazoEntregaDias": 2,
                          "seguro": 10.24,
                          "ajustePagamento": -20.60,
                          "totalFinal": 391.47,
                          "parcelas": 1,
                          "valorParcela": 391.47,
                          "creditoProximaCompra": 0.00,
                          "brinde": false
                        }
                        """))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":409.70")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")));
    }

    @Test
    void devolveErroComCodigo() throws Exception {
        String pedido = PEDIDO_EXEMPLO_1.replace("\"PIX\"", "\"CHEQUE\"");
        mvc.perform(post("/checkout/resumo").contentType("application/json").content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }
}
