package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolveOResumoDoExemploDoAnexo() throws Exception {
        String corpo = """
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // todos os valores em dinheiro com 2 casas decimais
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
    void escreveOsValoresComDuasCasasDecimais() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Bolsa", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.50 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":100.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"ajustePagamento\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")));
    }

    @Test
    void devolveSoOCodigoQuandoRecusaOPedido() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Mala", "precoUnitario": 300.00, "quantidade": 2, "pesoKg": 3.00 } ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void corpoIlegivelEhPedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{ nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }
}
