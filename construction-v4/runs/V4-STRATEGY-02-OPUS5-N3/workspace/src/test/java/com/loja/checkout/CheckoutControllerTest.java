package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolveOResumoNoFormatoCombinado() throws Exception {
        String resposta = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                }"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        igual(resposta, """
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
                }""");

        assertThat(resposta).contains("\"subtotalProdutos\":409.70", "\"frete\":0.00");
    }

    @Test
    void cupomAusenteNaoEhObrigatorio() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUL"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void devolveSoOCodigoQuandoRecusaOPedido() throws Exception {
        String resposta = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                                  "modalidadeEntrega": "DRONE",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUL"
                                }"""))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        igual(resposta, "{\"erro\": \"MODALIDADE_INVALIDA\"}");
    }

    @Test
    void corpoIlegivelEhPedidoInvalido() throws Exception {
        String resposta = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": "))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        igual(resposta, "{\"erro\": \"PEDIDO_INVALIDO\"}");
    }

    private static void igual(String recebido, String esperado) throws JSONException {
        JSONAssert.assertEquals(esperado, recebido, JSONCompareMode.STRICT);
    }
}
