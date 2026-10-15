package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Regras que os exemplos do financeiro nao cobrem. */
@SpringBootTest
@AutoConfigureMockMvc
class ResumoRegrasTest {

    private static final String CAMISETA_E_TENIS = """
            { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
            { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("FRETEGRATIS: o frete aparece no resumo e o desconto fica igual a ele")
    void freteGratisAparecComoDesconto() throws Exception {
        calcular("""
                {
                  "itens": [ %s ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "FRETEGRATIS",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 33.10,
                          "frete": 33.10,
                          "prazoEntregaDias": 2,
                          "seguro": 4.10,
                          "ajustePagamento": -20.69,
                          "totalFinal": 393.11,
                          "parcelas": 1,
                          "valorParcela": 393.11,
                          "creditoProximaCompra": 0.00,
                          "brinde": false
                        }
                        """, true));
    }

    @Test
    @DisplayName("ouro acima de R$ 500,00 em produtos leva brinde")
    void ouroAcimaDeQuinhentosLevaBrinde() throws Exception {
        calcular("""
                {
                  "itens": [ { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 3, "pesoKg": 1.20 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 749.70,
                          "descontoCupom": 0.00,
                          "frete": 0.00,
                          "prazoEntregaDias": 1,
                          "seguro": 7.50,
                          "ajustePagamento": -37.86,
                          "totalFinal": 719.34,
                          "parcelas": 1,
                          "valorParcela": 719.34,
                          "creditoProximaCompra": 37.48,
                          "brinde": true
                        }
                        """, true));
    }

    @Test
    @DisplayName("cartao em 1x nao tem juros e a parcela e o total do pedido")
    void cartaoAVistaSemJuros() throws Exception {
        calcular("""
                {
                  "itens": [ %s ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "ajustePagamento": 0.00,
                          "totalFinal": 413.80,
                          "parcelas": 1,
                          "valorParcela": 413.80
                        }
                        """));
    }

    @Test
    @DisplayName("cartao em 12x cobra juros pela tabela Price")
    void cartaoEmDozeVezesComJuros() throws Exception {
        calcular("""
                {
                  "itens": [ { "nome": "Jaqueta", "precoUnitario": 600.00, "quantidade": 1, "pesoKg": 1.00 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 12,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 600.00,
                          "frete": 0.00,
                          "seguro": 6.00,
                          "ajustePagamento": 81.24,
                          "totalFinal": 687.24,
                          "parcelas": 12,
                          "valorParcela": 57.27
                        }
                        """));
    }

    @Test
    @DisplayName("parcelas ausente vale 1")
    void parcelasAusenteValeUm() throws Exception {
        calcular("""
                {
                  "itens": [ %s ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"parcelas\": 1, \"valorParcela\": 413.80 }"));
    }

    @Test
    @DisplayName("motoboy leva pedido de exatamente 5 kg")
    void motoboyLevaCincoQuilos() throws Exception {
        calcular("""
                {
                  "itens": [ { "nome": "Bota", "precoUnitario": 100.00, "quantidade": 2, "pesoKg": 2.50 } ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"frete\": 18.00, \"prazoEntregaDias\": 0 }"));
    }

    private ResultActions calcular(String pedido) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pedido));
    }
}
