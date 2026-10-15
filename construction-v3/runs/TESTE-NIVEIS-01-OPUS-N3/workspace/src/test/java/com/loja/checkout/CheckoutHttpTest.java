package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O contrato que o site vê: /checkout/resumo, campos e formato. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolveOResumoComDuasCasasDecimais() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
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
                        """))
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")));
    }

    @Test
    void cupomEParcelasPodemNaoVir() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                          "modalidadeEntrega": "MOTOBOY",
                          "formaPagamento": "BOLETO",
                          "nivelClube": "BRONZE",
                          "regiao": "NORDESTE"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.totalFinal").value(429.29));
    }

    @Test
    void devolveSoOCodigoDoProblema() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{"nome": "Halter", "precoUnitario": 99.90, "quantidade": 2, "pesoKg": 2.60}],
                          "modalidadeEntrega": "MOTOBOY",
                          "formaPagamento": "PIX",
                          "nivelClube": "BRONZE",
                          "regiao": "SUDESTE"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void jsonIlegivelEPedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }
}
