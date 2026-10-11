package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutResumoTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
            {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Exemplo 1: expressa, BEMVINDO10, pix, bronze, norte")
    void exemplo1() throws Exception {
        resumoOk("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """.formatted(CAMISETA_E_TENIS), """
                {
                  "subtotalProdutos": 409.70,
                  "descontoCupom": 40.97,
                  "frete": 33.10,
                  "prazoEntregaDias": 2,
                  "seguro": 10.24,
                  "ajustePagamento": -20.60,
                  "totalFinal": 391.47,
                  "parcelas": 1,
                  "valorParcela": 391.47,
                  "creditoProximaCompra": 0.00,
                  "brinde": false
                }
                """);
    }

    @Test
    @DisplayName("Exemplo 2: economica, sem cupom, cartao 6x, prata, centro-oeste")
    void exemplo2() throws Exception {
        resumoOk("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6,
                  "nivelClube": "PRATA",
                  "regiao": "CENTRO_OESTE"
                }
                """.formatted(CAMISETA_E_TENIS), """
                {
                  "subtotalProdutos": 409.70,
                  "descontoCupom": 0.00,
                  "frete": 15.60,
                  "prazoEntregaDias": 7,
                  "seguro": 6.15,
                  "ajustePagamento": 30.55,
                  "totalFinal": 462.00,
                  "parcelas": 6,
                  "valorParcela": 77.00,
                  "creditoProximaCompra": 8.19,
                  "brinde": false
                }
                """);
    }

    @Test
    @DisplayName("Exemplo 3: motoboy, MENOS50, boleto, bronze, nordeste")
    void exemplo3() throws Exception {
        resumoOk("""
                {
                  "itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """, """
                {
                  "subtotalProdutos": 399.80,
                  "descontoCupom": 50.00,
                  "frete": 18.00,
                  "prazoEntregaDias": 0,
                  "seguro": 8.00,
                  "ajustePagamento": 3.49,
                  "totalFinal": 379.29,
                  "parcelas": 1,
                  "valorParcela": 379.29,
                  "creditoProximaCompra": 0.00,
                  "brinde": false
                }
                """);
    }

    @Test
    @DisplayName("Exemplo 4: retirada, LEVE3PAGUE2, cartao 3x sem juros, prata, sul")
    void exemplo4() throws Exception {
        resumoOk("""
                {
                  "itens": [
                    {"nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10},
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": "LEVE3PAGUE2",
                  "formaPagamento": "CARTAO",
                  "parcelas": 3,
                  "nivelClube": "PRATA",
                  "regiao": "SUL"
                }
                """, """
                {
                  "subtotalProdutos": 299.10,
                  "descontoCupom": 39.80,
                  "frete": 0.00,
                  "prazoEntregaDias": 1,
                  "seguro": 2.99,
                  "ajustePagamento": 0.00,
                  "totalFinal": 262.29,
                  "parcelas": 3,
                  "valorParcela": 87.43,
                  "creditoProximaCompra": 5.98,
                  "brinde": false
                }
                """);
    }

    @Test
    @DisplayName("Exemplo 5: ouro nao paga frete")
    void exemplo5() throws Exception {
        resumoOk("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS), """
                {
                  "subtotalProdutos": 409.70,
                  "descontoCupom": 0.00,
                  "frete": 0.00,
                  "prazoEntregaDias": 2,
                  "seguro": 4.10,
                  "ajustePagamento": -20.69,
                  "totalFinal": 393.11,
                  "parcelas": 1,
                  "valorParcela": 393.11,
                  "creditoProximaCompra": 20.48,
                  "brinde": false
                }
                """);
    }

    @Test
    @DisplayName("Exemplo do anexo: ouro com cupom BEMVINDO10 no pix")
    void exemploDoAnexo() throws Exception {
        resumoOk("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS), """
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
                """);
    }

    @Test
    @DisplayName("Todo valor em dinheiro sai com 2 casas decimais")
    void valoresComDuasCasas() throws Exception {
        String corpo = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Bone", "precoUnitario": 50.00, "quantidade": 2, "pesoKg": 0.50}],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "formaPagamento": "CARTAO",
                                  "parcelas": 1,
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo)
                .contains("\"subtotalProdutos\":100.00")
                .contains("\"frete\":0.00")
                .contains("\"totalFinal\":101.00")
                .contains("\"creditoProximaCompra\":0.00");
    }

    @Test
    @DisplayName("Brinde do ouro acima de R$ 500,00 em produtos")
    void brindeDoOuro() throws Exception {
        resumoOk("""
                {
                  "itens": [{"nome": "Jaqueta", "precoUnitario": 600.00, "quantidade": 1, "pesoKg": 1.00}],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """, """
                {
                  "subtotalProdutos": 600.00,
                  "frete": 0.00,
                  "seguro": 6.00,
                  "totalFinal": 575.70,
                  "creditoProximaCompra": 30.00,
                  "brinde": true
                }
                """);
    }

    @Test
    @DisplayName("FRETEGRATIS desconta exatamente o frete, que continua aparecendo no resumo")
    void freteGratis() throws Exception {
        resumoOk("""
                {
                  "itens": [{"nome": "Fone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 2.00}],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "FRETEGRATIS",
                  "formaPagamento": "CARTAO",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, """
                {
                  "subtotalProdutos": 100.00,
                  "descontoCupom": 34.00,
                  "frete": 34.00,
                  "seguro": 1.00,
                  "ajustePagamento": 0.00,
                  "totalFinal": 101.00,
                  "parcelas": 1,
                  "valorParcela": 101.00
                }
                """);
    }

    private void resumoOk(String pedido, String resumoEsperado) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().json(resumoEsperado));
    }
}
