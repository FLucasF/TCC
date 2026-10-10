package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ITENS_CAMISETA_TENIS = """
            [
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
            ]
            """;

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        String corpo = """
                {
                    "itens": %s,
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "BEMVINDO10",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "NORTE"
                }
                """.formatted(ITENS_CAMISETA_TENIS);

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
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
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() throws Exception {
        String corpo = """
                {
                    "itens": %s,
                    "modalidadeEntrega": "ECONOMICA",
                    "formaPagamento": "CARTAO",
                    "parcelas": 6,
                    "nivelClube": "PRATA",
                    "regiao": "CENTRO_OESTE"
                }
                """.formatted(ITENS_CAMISETA_TENIS);

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
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

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
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

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
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
    void exemplo5_expressaSemCupomPixOuroSudeste() throws Exception {
        String corpo = """
                {
                    "itens": %s,
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "OURO",
                    "regiao": "SUDESTE"
                }
                """.formatted(ITENS_CAMISETA_TENIS);

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
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
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        String corpo = """
                {
                    "itens": [],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void motoboyAcimaDoPesoRetornaModalidadeIndisponivel() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Caixa pesada", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 6.0}
                    ],
                    "modalidadeEntrega": "MOTOBOY",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomInexistenteRetornaCupomInvalido() throws Exception {
        String corpo = """
                {
                    "itens": %s,
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "NAOEXISTE",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """.formatted(ITENS_CAMISETA_TENIS);

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "MENOS50",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Tenis caro", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 1.0}
                    ],
                    "modalidadeEntrega": "RETIRADA_LOJA",
                    "formaPagamento": "BOLETO",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void boletoComParcelasRetornaParcelamentoInvalido() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30}
                    ],
                    "modalidadeEntrega": "RETIRADA_LOJA",
                    "formaPagamento": "BOLETO",
                    "parcelas": 2,
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }
}
