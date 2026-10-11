package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    private static final String PEDIDO_DO_ANEXO = """
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
    void devolveOResumoDoAnexoComDuasCasasDecimais() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(PEDIDO_DO_ANEXO))
                .andExpect(status().isOk())
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
    void pedidoSemCupomEsemParcelas() throws Exception {
        String pedido = """
                {
                  "itens": [ { "nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25 } ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.totalFinal").value(429.29));
    }

    @Test
    void devolveSoOcodigoDoProblema() throws Exception {
        String pedido = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void corpoSemItensEpedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void corpoAusenteEpedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void corpoMalFormadoEpedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{ nao e json"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
