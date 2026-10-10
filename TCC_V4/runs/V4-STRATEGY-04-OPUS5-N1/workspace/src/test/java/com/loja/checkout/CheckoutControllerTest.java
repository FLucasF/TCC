package com.loja.checkout;

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
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolve_o_resumo_da_compra_do_anexo() throws Exception {
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-18.64))
                .andExpect(jsonPath("$.totalFinal").value(354.19))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(354.19))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void valores_em_dinheiro_saem_com_duas_casas() throws Exception {
        String corpo = """
                {
                  "itens": [{"nome": "Meia", "precoUnitario": 20.00, "quantidade": 1, "pesoKg": 0.10}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":20.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")));
    }

    @Test
    void cupom_que_nao_existe_devolve_so_o_codigo_do_problema() throws Exception {
        String corpo = """
                {
                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "PROMO99",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void corpo_ilegivel_devolve_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{ nao e json"))
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
