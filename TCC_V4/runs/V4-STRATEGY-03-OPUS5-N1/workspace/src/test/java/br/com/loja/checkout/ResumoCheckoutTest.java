package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
            {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    @DisplayName("Exemplo do anexo: EXPRESSA, BEMVINDO10, PIX, OURO, SUDESTE")
    void exemploDoAnexo() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "EXPRESSA", "cupom": "BEMVINDO10",
                 "formaPagamento": "PIX", "parcelas": 1,
                 "nivelClube": "OURO", "regiao": "SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
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
    @DisplayName("Exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
    void exemplo1() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "EXPRESSA", "cupom": "BEMVINDO10",
                 "formaPagamento": "PIX",
                 "nivelClube": "BRONZE", "regiao": "NORTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(10.24))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(391.47))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
    void exemplo2() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "ECONOMICA",
                 "formaPagamento": "CARTAO", "parcelas": 6,
                 "nivelClube": "PRATA", "regiao": "CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.seguro").value(6.15))
                .andExpect(jsonPath("$.ajustePagamento").value(30.55))
                .andExpect(jsonPath("$.totalFinal").value(462.00))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(77.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(8.19))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
    void exemplo3() throws Exception {
        resumo("""
                {"itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                 "modalidadeEntrega": "MOTOBOY", "cupom": "MENOS50",
                 "formaPagamento": "BOLETO",
                 "nivelClube": "BRONZE", "regiao": "NORDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.seguro").value(8.00))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(379.29))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(379.29))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
    void exemplo4() throws Exception {
        resumo("""
                {"itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10},
                           {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                 "modalidadeEntrega": "RETIRADA_LOJA", "cupom": "LEVE3PAGUE2",
                 "formaPagamento": "CARTAO", "parcelas": 3,
                 "nivelClube": "PRATA", "regiao": "SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.seguro").value(2.99))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(262.29))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(87.43))
                .andExpect(jsonPath("$.creditoProximaCompra").value(5.98))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 5: EXPRESSA, sem cupom, PIX, OURO, SUDESTE")
    void exemplo5() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "EXPRESSA",
                 "formaPagamento": "PIX",
                 "nivelClube": "OURO", "regiao": "SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(jsonPath("$.totalFinal").value(393.11))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(393.11))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("FRETEGRATIS: frete aparece no resumo e o desconto fica igual a ele")
    void freteGratisDescontaOFrete() throws Exception {
        resumo("""
                {"itens": [%s],
                 "modalidadeEntrega": "EXPRESSA", "cupom": "FRETEGRATIS",
                 "formaPagamento": "BOLETO",
                 "nivelClube": "BRONZE", "regiao": "SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.totalFinal").value(417.29));
    }

    @Test
    @DisplayName("OURO acima de 500 em produtos ganha brinde")
    void ouroGanhaBrinde() throws Exception {
        resumo("""
                {"itens": [{"nome": "Jaqueta", "precoUnitario": 600.00, "quantidade": 1, "pesoKg": 1.50}],
                 "modalidadeEntrega": "ECONOMICA",
                 "formaPagamento": "PIX",
                 "nivelClube": "OURO", "regiao": "SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.brinde").value(true))
                .andExpect(jsonPath("$.creditoProximaCompra").value(30.00));
    }

    @Test
    @DisplayName("Cartao em 12x cobra juros de tabela Price")
    void cartaoEmDozeVezes() throws Exception {
        resumo("""
                {"itens": [{"nome": "Fone", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.25}],
                 "modalidadeEntrega": "RETIRADA_LOJA",
                 "formaPagamento": "CARTAO", "parcelas": 12,
                 "nivelClube": "BRONZE", "regiao": "SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(114.48))
                .andExpect(jsonPath("$.parcelas").value(12))
                .andExpect(jsonPath("$.valorParcela").value(9.54))
                .andExpect(jsonPath("$.ajustePagamento").value(13.48));
    }
}
