package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutResumoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """;

        postResumo(corpo)
                .andExpect(MockMvcResultMatchers.jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(MockMvcResultMatchers.jsonPath("$.descontoCupom").value(40.97))
                .andExpect(MockMvcResultMatchers.jsonPath("$.frete").value(33.10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.seguro").value(10.24))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalFinal").value(391.47))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parcelas").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.valorParcela").value(391.47))
                .andExpect(MockMvcResultMatchers.jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6,
                  "nivelClube": "PRATA",
                  "regiao": "CENTRO_OESTE"
                }
                """;

        postResumo(corpo)
                .andExpect(MockMvcResultMatchers.jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(MockMvcResultMatchers.jsonPath("$.descontoCupom").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.frete").value(15.60))
                .andExpect(MockMvcResultMatchers.jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(MockMvcResultMatchers.jsonPath("$.seguro").value(6.15))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ajustePagamento").value(30.55))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalFinal").value(462.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parcelas").value(6))
                .andExpect(MockMvcResultMatchers.jsonPath("$.valorParcela").value(77.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.creditoProximaCompra").value(8.19))
                .andExpect(MockMvcResultMatchers.jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """;

        postResumo(corpo)
                .andExpect(MockMvcResultMatchers.jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(MockMvcResultMatchers.jsonPath("$.descontoCupom").value(50.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.frete").value(18.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.seguro").value(8.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalFinal").value(379.29))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parcelas").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.valorParcela").value(379.29))
                .andExpect(MockMvcResultMatchers.jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo4_retiradaLojaLeve3Pague2Cartao3xPrataSul() throws Exception {
        String corpo = """
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
                """;

        postResumo(corpo)
                .andExpect(MockMvcResultMatchers.jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.descontoCupom").value(39.80))
                .andExpect(MockMvcResultMatchers.jsonPath("$.frete").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.seguro").value(2.99))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalFinal").value(262.29))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parcelas").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.valorParcela").value(87.43))
                .andExpect(MockMvcResultMatchers.jsonPath("$.creditoProximaCompra").value(5.98))
                .andExpect(MockMvcResultMatchers.jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """;

        postResumo(corpo)
                .andExpect(MockMvcResultMatchers.jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(MockMvcResultMatchers.jsonPath("$.descontoCupom").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.frete").value(0.00))
                .andExpect(MockMvcResultMatchers.jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.seguro").value(4.10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(MockMvcResultMatchers.jsonPath("$.totalFinal").value(393.11))
                .andExpect(MockMvcResultMatchers.jsonPath("$.parcelas").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.valorParcela").value(393.11))
                .andExpect(MockMvcResultMatchers.jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(MockMvcResultMatchers.jsonPath("$.brinde").value(false));
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        postResumo(corpo).andExpect(MockMvcResultMatchers.jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Caixa pesada", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 6.00}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        postResumo(corpo).andExpect(MockMvcResultMatchers.jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomDesconhecidoRetornaCupomInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.50}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "NAOEXISTE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        postResumo(corpo).andExpect(MockMvcResultMatchers.jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.50}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        postResumo(corpo).andExpect(MockMvcResultMatchers.jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Notebook", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.00}
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        postResumo(corpo).andExpect(MockMvcResultMatchers.jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void boletoComDuasParcelasRetornaParcelamentoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.50}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "BOLETO",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        postResumo(corpo).andExpect(MockMvcResultMatchers.jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    private org.springframework.test.web.servlet.ResultActions postResumo(String corpo) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
