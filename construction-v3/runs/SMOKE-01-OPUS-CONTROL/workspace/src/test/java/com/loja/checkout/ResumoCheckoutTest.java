package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Os exemplos conferidos pelo financeiro, ponta a ponta. */
@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_E_TENIS = """
            { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
            { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
            """;

    @Autowired
    private MockMvc mockMvc;

    private void esperaResumo(String pedido, String resumo) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().json(resumo, true));
    }

    private void esperaErro(String pedido, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"" + codigo + "\"}", true));
    }

    @Test
    @DisplayName("Exemplo 1: expressa, BEMVINDO10 e Pix")
    void exemploUm() throws Exception {
        esperaResumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1
                }
                """.formatted(CAMISETA_E_TENIS), """
                {
                  "subtotalProdutos": 409.70,
                  "descontoCupom": 40.97,
                  "frete": 33.10,
                  "prazoEntregaDias": 2,
                  "ajustePagamento": -20.09,
                  "totalFinal": 381.74,
                  "parcelas": 1,
                  "valorParcela": 381.74
                }
                """);
    }

    @Test
    @DisplayName("Exemplo 2: economica, sem cupom, cartao em 6x com juros")
    void exemploDois() throws Exception {
        esperaResumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6
                }
                """.formatted(CAMISETA_E_TENIS), """
                {
                  "subtotalProdutos": 409.70,
                  "descontoCupom": 0.00,
                  "frete": 15.60,
                  "prazoEntregaDias": 7,
                  "ajustePagamento": 30.10,
                  "totalFinal": 455.40,
                  "parcelas": 6,
                  "valorParcela": 75.90
                }
                """);
    }

    @Test
    @DisplayName("Exemplo 3: motoboy, MENOS50 e boleto")
    void exemploTres() throws Exception {
        esperaResumo("""
                {
                  "itens": [{ "nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25 }],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO"
                }
                """, """
                {
                  "subtotalProdutos": 399.80,
                  "descontoCupom": 50.00,
                  "frete": 18.00,
                  "prazoEntregaDias": 0,
                  "ajustePagamento": 3.49,
                  "totalFinal": 371.29,
                  "parcelas": 1,
                  "valorParcela": 371.29
                }
                """);
    }

    @Test
    @DisplayName("Exemplo 4: retirada na loja, LEVE3PAGUE2 e cartao em 3x sem juros")
    void exemploQuatro() throws Exception {
        esperaResumo("""
                {
                  "itens": [
                    { "nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10 },
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": "LEVE3PAGUE2",
                  "formaPagamento": "CARTAO",
                  "parcelas": 3
                }
                """, """
                {
                  "subtotalProdutos": 299.10,
                  "descontoCupom": 39.80,
                  "frete": 0.00,
                  "prazoEntregaDias": 1,
                  "ajustePagamento": 0.00,
                  "totalFinal": 259.30,
                  "parcelas": 3,
                  "valorParcela": 86.43
                }
                """);
    }

    @Test
    @DisplayName("FRETEGRATIS abate exatamente o valor do frete, que continua aparecendo no resumo")
    void freteGratis() throws Exception {
        esperaResumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "FRETEGRATIS",
                  "formaPagamento": "CARTAO",
                  "parcelas": 2
                }
                """.formatted(CAMISETA_E_TENIS), """
                {
                  "subtotalProdutos": 409.70,
                  "descontoCupom": 33.10,
                  "frete": 33.10,
                  "prazoEntregaDias": 2,
                  "ajustePagamento": 0.00,
                  "totalFinal": 409.70,
                  "parcelas": 2,
                  "valorParcela": 204.85
                }
                """);
    }

    @Test
    @DisplayName("Sem parcelas informadas o pedido e tratado como a vista")
    void parcelasAusentes() throws Exception {
        esperaResumo("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": null,
                  "formaPagamento": "CARTAO"
                }
                """, """
                {
                  "subtotalProdutos": 19.90,
                  "descontoCupom": 0.00,
                  "frete": 0.00,
                  "prazoEntregaDias": 1,
                  "ajustePagamento": 0.00,
                  "totalFinal": 19.90,
                  "parcelas": 1,
                  "valorParcela": 19.90
                }
                """);
    }

    @Test
    @DisplayName("Carrinho vazio ou item com numero invalido")
    void pedidoInvalido() throws Exception {
        esperaErro("""
                { "itens": [], "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX" }
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 0, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX"
                }
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": -1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX"
                }
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX"
                }
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                { "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX" }
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Entrega inexistente ou nao informada")
    void modalidadeInvalida() throws Exception {
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "DRONE", "formaPagamento": "PIX"
                }
                """, "MODALIDADE_INVALIDA");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "formaPagamento": "PIX"
                }
                """, "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("Motoboy nao leva pedido acima de 5 kg")
    void modalidadeIndisponivel() throws Exception {
        esperaErro("""
                {
                  "itens": [{ "nome": "Halter", "precoUnitario": 99.90, "quantidade": 3, "pesoKg": 2.00 }],
                  "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX"
                }
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("Motoboy leva pedido de exatamente 5 kg")
    void motoboyNoLimite() throws Exception {
        esperaResumo("""
                {
                  "itens": [{ "nome": "Halter", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 5.00 }],
                  "modalidadeEntrega": "MOTOBOY", "formaPagamento": "CARTAO"
                }
                """, """
                {
                  "subtotalProdutos": 100.00, "descontoCupom": 0.00, "frete": 18.00,
                  "prazoEntregaDias": 0, "ajustePagamento": 0.00, "totalFinal": 118.00,
                  "parcelas": 1, "valorParcela": 118.00
                }
                """);
    }

    @Test
    @DisplayName("Cupom que nao existe e cupom fora da condicao")
    void errosDeCupom() throws Exception {
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "cupom": "NATAL2020", "formaPagamento": "PIX"
                }
                """, "CUPOM_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "cupom": "bemvindo10", "formaPagamento": "PIX"
                }
                """, "CUPOM_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "cupom": "MENOS50", "formaPagamento": "PIX"
                }
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("MENOS50 vale a partir de exatamente R$ 300,00 em produtos")
    void menos50NoLimite() throws Exception {
        esperaResumo("""
                {
                  "itens": [{ "nome": "Jaqueta", "precoUnitario": 300.00, "quantidade": 1, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "MENOS50", "formaPagamento": "CARTAO"
                }
                """, """
                {
                  "subtotalProdutos": 300.00, "descontoCupom": 50.00, "frete": 0.00,
                  "prazoEntregaDias": 1, "ajustePagamento": 0.00, "totalFinal": 250.00,
                  "parcelas": 1, "valorParcela": 250.00
                }
                """);
    }

    @Test
    @DisplayName("Forma de pagamento inexistente ou nao informada")
    void formaPagamentoInvalida() throws Exception {
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "DINHEIRO"
                }
                """, "FORMA_PAGAMENTO_INVALIDA");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA"
                }
                """, "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    @DisplayName("Parcelamento fora do permitido")
    void parcelamentoInvalido() throws Exception {
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX", "parcelas": 2
                }
                """, "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "BOLETO", "parcelas": 3
                }
                """, "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "CARTAO", "parcelas": 13
                }
                """, "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "formaPagamento": "CARTAO", "parcelas": 0
                }
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Boleto nao atende pedido acima de R$ 1.000,00")
    void boletoAcimaDoTeto() throws Exception {
        esperaErro("""
                {
                  "itens": [{ "nome": "Casaco", "precoUnitario": 1500.00, "quantidade": 1, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "BOLETO"
                }
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("Boleto vale para pedido de exatamente R$ 1.000,00")
    void boletoNoTeto() throws Exception {
        esperaResumo("""
                {
                  "itens": [{ "nome": "Casaco", "precoUnitario": 1000.00, "quantidade": 1, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "BOLETO"
                }
                """, """
                {
                  "subtotalProdutos": 1000.00, "descontoCupom": 0.00, "frete": 0.00,
                  "prazoEntregaDias": 1, "ajustePagamento": 3.49, "totalFinal": 1003.49,
                  "parcelas": 1, "valorParcela": 1003.49
                }
                """);
    }

    @Test
    @DisplayName("Os erros saem na ordem combinada com o site")
    void ordemDosErros() throws Exception {
        // Tudo errado de uma vez: sai o primeiro da lista.
        esperaErro("""
                {
                  "itens": [], "modalidadeEntrega": "DRONE", "cupom": "NATAL2020",
                  "formaPagamento": "DINHEIRO", "parcelas": 99
                }
                """, "PEDIDO_INVALIDO");
        // Carrinho ok, resto errado: sai o erro da entrega.
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "DRONE", "cupom": "NATAL2020",
                  "formaPagamento": "DINHEIRO", "parcelas": 99
                }
                """, "MODALIDADE_INVALIDA");
        // Entrega ok mas indisponivel, cupom e pagamento errados: sai a indisponibilidade.
        esperaErro("""
                {
                  "itens": [{ "nome": "Halter", "precoUnitario": 99.90, "quantidade": 3, "pesoKg": 2.00 }],
                  "modalidadeEntrega": "MOTOBOY", "cupom": "NATAL2020", "formaPagamento": "DINHEIRO"
                }
                """, "MODALIDADE_INDISPONIVEL");
        // Cupom inexistente tem prioridade sobre o pagamento inexistente.
        esperaErro("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "EXPRESSA", "cupom": "NATAL2020", "formaPagamento": "DINHEIRO"
                }
                """, "CUPOM_INVALIDO");
        // Parcelamento invalido tem prioridade sobre o teto do boleto.
        esperaErro("""
                {
                  "itens": [{ "nome": "Casaco", "precoUnitario": 2000.00, "quantidade": 1, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA", "formaPagamento": "BOLETO", "parcelas": 2
                }
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Corpo mal formado tambem e pedido invalido")
    void corpoMalFormado() throws Exception {
        esperaErro("isso nao e json", "PEDIDO_INVALIDO");
    }
}
