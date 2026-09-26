package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String EXEMPLO_5 = """
            {
              "itens": [
                { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
              ],
              "modalidadeEntrega": "EXPRESSA",
              "formaPagamento": "PIX",
              "parcelas": 1,
              "nivelClube": "OURO",
              "regiao": "SUDESTE"
            }
            """;

    @Test
    void devolve_o_resumo_com_dois_decimais() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EXEMPLO_5))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 0.00,
                          "frete": 0.00,
                          "prazoEntregaDias": 2,
                          "imposto": 49.16,
                          "ajustePagamento": -22.94,
                          "totalFinal": 435.92,
                          "parcelas": 1,
                          "valorParcela": 435.92,
                          "creditoProximaCompra": 20.48,
                          "brinde": false
                        }
                        """, true))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":409.70")));
    }

    @Test
    void cupom_pode_vir_nulo() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{ "nome": "Bone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.10 }],
                          "modalidadeEntrega": "RETIRADA_LOJA",
                          "cupom": null,
                          "formaPagamento": "PIX",
                          "nivelClube": "BRONZE",
                          "regiao": "SUDESTE"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1));
    }

    @Test
    void devolve_o_codigo_do_erro_com_400() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{ "nome": "Halter", "precoUnitario": 100.00, "quantidade": 6, "pesoKg": 1.00 }],
                          "modalidadeEntrega": "MOTOBOY",
                          "formaPagamento": "PIX",
                          "nivelClube": "BRONZE",
                          "regiao": "SUDESTE"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void corpo_ilegivel_vira_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
