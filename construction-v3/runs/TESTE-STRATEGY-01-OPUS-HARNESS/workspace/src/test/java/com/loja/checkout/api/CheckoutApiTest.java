package com.loja.checkout.api;

import org.hamcrest.Matchers;
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
class CheckoutApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String PEDIDO_DO_ANEXO = """
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
    void devolveOResumoComDuasCasasDecimais() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(PEDIDO_DO_ANEXO))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.imposto").value(49.16))
                .andExpect(jsonPath("$.ajustePagamento").value(-22.94))
                .andExpect(jsonPath("$.totalFinal").value(435.92))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(435.92))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false))
                .andExpect(content().string(Matchers.containsString("\"frete\":0.00")));
    }

    @Test
    void cupomNuloEAceito() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 3, "pesoKg": 0.10 }],
                          "modalidadeEntrega": "RETIRADA_LOJA",
                          "cupom": null,
                          "formaPagamento": "CARTAO",
                          "nivelClube": "PRATA",
                          "regiao": "NORDESTE"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.creditoProximaCompra").value(1.19));
    }

    @Test
    void erroVemComoCodigoEStatus400() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{ "nome": "Halteres", "precoUnitario": 99.90, "quantidade": 3, "pesoKg": 2.00 }],
                          "modalidadeEntrega": "MOTOBOY",
                          "formaPagamento": "PIX",
                          "nivelClube": "BRONZE",
                          "regiao": "SUL"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}"));
    }

    @Test
    void corpoIlegivelEPedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{ nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}"));
    }

    @Test
    void corpoAusenteEPedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}"));
    }
}
