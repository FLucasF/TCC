package br.com.loja.checkout;

import static org.hamcrest.Matchers.containsString;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolve_o_resumo_com_duas_casas_decimais() throws Exception {
        String pedido = """
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
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
                        """, true))
                .andExpect(content().string(containsString("\"frete\":0.00")))
                .andExpect(content().string(containsString("\"totalFinal\":354.19")));
    }

    @Test
    void pedido_sem_cupom_e_sem_parcelas_e_calculado_a_vista() throws Exception {
        String pedido = """
                {
                  "itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"descontoCupom\":0.00")))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(content().string(containsString("\"totalFinal\":429.29")));
    }

    @Test
    void pedido_recusado_devolve_so_o_codigo_do_problema() throws Exception {
        String pedido = """
                {
                  "itens": [{"nome": "Mala", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 9.00}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void corpo_ilegivel_e_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{\"itens\": 5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }
}
