package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    private static final String PEDIDO = """
            {
              "itens": [
                { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
              ],
              "modalidadeEntrega": "EXPRESSA",
              "cupom": "BEMVINDO10",
              "formaPagamento": "PIX",
              "parcelas": 1,
              "nivelClube": "OURO",
              "regiao": "SUDESTE"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolve_o_resumo_com_duas_casas_decimais() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 40.97,
                          "frete": 0.00,
                          "prazoEntregaDias": 2,
                          "seguro": 4.10,
                          "ajustePagamento": -18.64,
                          "totalFinal": 354.19,
                          "parcelas": 1,
                          "valorParcela": 354.19,
                          "creditoProximaCompra": 20.48,
                          "brinde": false
                        }
                        """, true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("409.70")));
    }

    @Test
    void parcelas_ausentes_valem_um() throws Exception {
        String semParcelas = PEDIDO.replace("\"parcelas\": 1,", "");
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(semParcelas))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(1));
    }

    @Test
    void devolve_so_o_codigo_do_problema_quando_recusa() throws Exception {
        String comCupomInvalido = PEDIDO.replace("BEMVINDO10", "PROMOCAODOZE");
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(comCupomInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"CUPOM_INVALIDO\"}", true));
    }

    @Test
    void corpo_ilegivel_tambem_e_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }
}
