package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutApiTest {

    private static final String CAMISETA_E_TENIS = """
            { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
            { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
            """;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    @DisplayName("Exemplo 1: EXPRESSA com BEMVINDO10 no Pix")
    void exemplo1() throws Exception {
        resumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                // imposto: 12% sobre 409,70 - 40,97 = 368,73
                .andExpect(jsonPath("$.imposto").value(44.25))
                // total do pedido 446,08; Pix desconta 5% = 22,30
                .andExpect(jsonPath("$.ajustePagamento").value(-22.30))
                .andExpect(jsonPath("$.totalFinal").value(423.78))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(423.78))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 2: ECONOMICA sem cupom, cartao em 6x com juros")
    void exemplo2() throws Exception {
        resumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                // 12,00 + 2,00 x 1,8 kg
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.imposto").value(49.16))
                // total do pedido 474,46; parcela Price 1,99% em 6x = 84,67
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(84.67))
                .andExpect(jsonPath("$.totalFinal").value(508.02))
                .andExpect(jsonPath("$.ajustePagamento").value(33.56));
    }

    @Test
    @DisplayName("Exemplo 3: MOTOBOY com MENOS50 no boleto")
    void exemplo3() throws Exception {
        resumo("""
                {
                  "itens": [{ "nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25 }],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                // 7% sobre 349,80
                .andExpect(jsonPath("$.imposto").value(24.49))
                // total do pedido 392,29 + tarifa de 3,49
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(395.78))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(395.78));
    }

    @Test
    @DisplayName("Exemplo 4: RETIRADA_LOJA com LEVE3PAGUE2, cartao em 3x sem juros")
    void exemplo4() throws Exception {
        resumo("""
                {
                  "itens": [
                    { "nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10 },
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": "LEVE3PAGUE2",
                  "formaPagamento": "CARTAO",
                  "parcelas": 3,
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                // 2 meias gratis (7 / 3), nenhuma camiseta
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                // 11% sobre 259,30
                .andExpect(jsonPath("$.imposto").value(28.52))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(287.82))
                .andExpect(jsonPath("$.parcelas").value(3))
                // sem juros: parcela e o total dividido por 3, arredondado
                .andExpect(jsonPath("$.valorParcela").value(95.94));
    }

    @Test
    @DisplayName("Exemplo 5: cliente OURO nao paga frete e ganha credito")
    void exemplo5() throws Exception {
        resumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": null,
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
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
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("FRETEGRATIS: o frete aparece e o desconto fica igual a ele")
    void freteGratis() throws Exception {
        resumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "FRETEGRATIS",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                // 7% sobre 376,60
                .andExpect(jsonPath("$.imposto").value(26.36))
                .andExpect(jsonPath("$.totalFinal").value(414.26));
    }

    @Test
    @DisplayName("PRATA ganha 2% em credito e paga frete")
    void clubePrata() throws Exception {
        resumo("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "PRATA",
                  "regiao": "CENTRO_OESTE"
                }
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.creditoProximaCompra").value(8.19))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("OURO acima de R$ 500,00 em produtos ganha brinde")
    void clubeOuroComBrinde() throws Exception {
        resumo("""
                {
                  "itens": [{ "nome": "Jaqueta", "precoUnitario": 299.90, "quantidade": 2, "pesoKg": 1.00 }],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(29.99))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    @DisplayName("Parcelas ausentes contam como 1")
    void parcelasAusentesContamComoUma() throws Exception {
        resumo("""
                {
                  "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(1))
                // 19,90 + 7% (1,393 -> 1,39)
                .andExpect(jsonPath("$.imposto").value(1.39))
                .andExpect(jsonPath("$.totalFinal").value(21.29))
                .andExpect(jsonPath("$.valorParcela").value(21.29))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00));
    }
}
