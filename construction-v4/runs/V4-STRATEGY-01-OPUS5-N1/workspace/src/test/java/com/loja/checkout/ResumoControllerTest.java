package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    private static final String PEDIDO_DO_ANEXO = """
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolve_o_resumo_com_todos_os_valores_em_duas_casas() throws Exception {
        String corpo = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO_DO_ANEXO))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.brinde").value(false))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).contains("\"subtotalProdutos\":409.70", "\"descontoCupom\":40.97",
                "\"frete\":0.00", "\"seguro\":4.10", "\"ajustePagamento\":-18.64",
                "\"totalFinal\":354.19", "\"valorParcela\":354.19",
                "\"creditoProximaCompra\":20.48");
    }

    @Test
    void sem_cupom_e_sem_parcelas_o_pedido_e_aceito() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Fone", "precoUnitario": 199.90,
                                             "quantidade": 2, "pesoKg": 0.25}],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "BOLETO",
                                  "nivelClube": "BRONZE",
                                  "regiao": "NORDESTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.totalFinal").value(429.29));
    }

    @Test
    void pedido_recusado_devolve_so_o_codigo_do_problema() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Mala", "precoUnitario": 10.00,
                                             "quantidade": 6, "pesoKg": 1.00}],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void corpo_ilegivel_e_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }
}
