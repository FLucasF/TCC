package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** O formato do que o site recebe de volta, no sucesso e na recusa. */
@SpringBootTest
class CheckoutControllerTest {

    private MockMvc mockMvc;

    @Autowired
    void configurar(WebApplicationContext contexto) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void devolve_o_resumo_com_duas_casas() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
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
                        """, true));
    }

    @Test
    void devolve_so_o_codigo_do_problema() throws Exception {
        String corpo = """
                {
                  "itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "ATLANTIDA"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"REGIAO_INVALIDA\"}", true));
    }

    @Test
    void sem_cupom_e_sem_parcelas_os_campos_podem_nao_vir() throws Exception {
        String corpo = """
                {
                  "itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.totalFinal").value(429.29));
    }
}
