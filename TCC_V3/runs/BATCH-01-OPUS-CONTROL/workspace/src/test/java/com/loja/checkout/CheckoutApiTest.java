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

/** Confere o contrato combinado com o desenvolvedor do site. */
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions chamar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    void devolveOResumoDoExemploDoAnexo() throws Exception {
        chamar("""
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                    { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1
                }
                """)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.09))
                .andExpect(jsonPath("$.totalFinal").value(381.74))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(381.74));
    }

    @Test
    void escreveOsValoresComDuasCasasDecimais() throws Exception {
        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.20 }],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 2
                }
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 100.00,
                          "descontoCupom": 0.00,
                          "frete": 0.00,
                          "prazoEntregaDias": 1,
                          "ajustePagamento": 0.00,
                          "totalFinal": 100.00,
                          "parcelas": 2,
                          "valorParcela": 50.00
                        }
                        """))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":100.00")));
    }

    @Test
    void cupomEParcelasSaoOpcionais() throws Exception {
        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.20 }],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": null,
                  "formaPagamento": "BOLETO"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(121.49))
                .andExpect(jsonPath("$.parcelas").value(1));
    }

    @Test
    void devolve400ComOCodigoDoErro() throws Exception {
        chamar("""
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));

        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.20 }],
                  "modalidadeEntrega": "TELETRANSPORTE",
                  "formaPagamento": "PIX"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));

        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.20 }],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "PROMOFANTASMA",
                  "formaPagamento": "PIX"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));

        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.20 }],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "parcelas": 3
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void corpoMalFormadoViraPedidoInvalido() throws Exception {
        chamar("{ nao e json }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
