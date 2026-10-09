package com.loja.checkout;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ITENS_PADRAO = """
            "itens": [
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
            ]
            """;

    @Test
    void exemplo1_expressaComCupomBemvindo10Pix() throws Exception {
        String corpo = """
                {
                    %s,
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "BEMVINDO10",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "NORTE"
                }
                """.formatted(ITENS_PADRAO);

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(409.70)))
                .andExpect(jsonPath("$.descontoCupom", is(40.97)))
                .andExpect(jsonPath("$.frete", is(33.10)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(2)))
                .andExpect(jsonPath("$.seguro", is(10.24)))
                .andExpect(jsonPath("$.ajustePagamento", is(-20.60)))
                .andExpect(jsonPath("$.totalFinal", is(391.47)))
                .andExpect(jsonPath("$.parcelas", is(1)))
                .andExpect(jsonPath("$.valorParcela", is(391.47)))
                .andExpect(jsonPath("$.creditoProximaCompra", is(0.00)))
                .andExpect(jsonPath("$.brinde", is(false)));
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrata() throws Exception {
        String corpo = """
                {
                    %s,
                    "modalidadeEntrega": "ECONOMICA",
                    "formaPagamento": "CARTAO",
                    "parcelas": 6,
                    "nivelClube": "PRATA",
                    "regiao": "CENTRO_OESTE"
                }
                """.formatted(ITENS_PADRAO);

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(409.70)))
                .andExpect(jsonPath("$.descontoCupom", is(0.00)))
                .andExpect(jsonPath("$.frete", is(15.60)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(7)))
                .andExpect(jsonPath("$.seguro", is(6.15)))
                .andExpect(jsonPath("$.ajustePagamento", is(30.55)))
                .andExpect(jsonPath("$.totalFinal", is(462.00)))
                .andExpect(jsonPath("$.parcelas", is(6)))
                .andExpect(jsonPath("$.valorParcela", is(77.00)))
                .andExpect(jsonPath("$.creditoProximaCompra", is(8.19)))
                .andExpect(jsonPath("$.brinde", is(false)));
    }

    @Test
    void exemplo3_motoboyMenos50Boleto() throws Exception {
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(399.80)))
                .andExpect(jsonPath("$.descontoCupom", is(50.00)))
                .andExpect(jsonPath("$.frete", is(18.00)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(0)))
                .andExpect(jsonPath("$.seguro", is(8.00)))
                .andExpect(jsonPath("$.ajustePagamento", is(3.49)))
                .andExpect(jsonPath("$.totalFinal", is(379.29)))
                .andExpect(jsonPath("$.parcelas", is(1)))
                .andExpect(jsonPath("$.valorParcela", is(379.29)))
                .andExpect(jsonPath("$.creditoProximaCompra", is(0.00)))
                .andExpect(jsonPath("$.brinde", is(false)));
    }

    @Test
    void exemplo4_retiradaLojaLeve3Pague2Cartao3xPrata() throws Exception {
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(299.10)))
                .andExpect(jsonPath("$.descontoCupom", is(39.80)))
                .andExpect(jsonPath("$.frete", is(0.00)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(1)))
                .andExpect(jsonPath("$.seguro", is(2.99)))
                .andExpect(jsonPath("$.ajustePagamento", is(0.00)))
                .andExpect(jsonPath("$.totalFinal", is(262.29)))
                .andExpect(jsonPath("$.parcelas", is(3)))
                .andExpect(jsonPath("$.valorParcela", is(87.43)))
                .andExpect(jsonPath("$.creditoProximaCompra", is(5.98)))
                .andExpect(jsonPath("$.brinde", is(false)));
    }

    @Test
    void exemplo5_expressaSemCupomPixOuro() throws Exception {
        String corpo = """
                {
                    %s,
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "OURO",
                    "regiao": "SUDESTE"
                }
                """.formatted(ITENS_PADRAO);

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(409.70)))
                .andExpect(jsonPath("$.descontoCupom", is(0.00)))
                .andExpect(jsonPath("$.frete", is(0.00)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(2)))
                .andExpect(jsonPath("$.seguro", is(4.10)))
                .andExpect(jsonPath("$.ajustePagamento", is(-20.69)))
                .andExpect(jsonPath("$.totalFinal", is(393.11)))
                .andExpect(jsonPath("$.parcelas", is(1)))
                .andExpect(jsonPath("$.valorParcela", is(393.11)))
                .andExpect(jsonPath("$.creditoProximaCompra", is(20.48)))
                .andExpect(jsonPath("$.brinde", is(false)));
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void nivelClubeInvalidoTemPrioridadeSobreRestante() throws Exception {
        String corpo = """
                {
                    %s,
                    "modalidadeEntrega": "MODALIDADE_QUE_NAO_EXISTE",
                    "formaPagamento": "PIX",
                    "nivelClube": "DIAMANTE",
                    "regiao": "SUDESTE"
                }
                """.formatted(ITENS_PADRAO);

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("NIVEL_CLUBE_INVALIDO")));
    }

    @Test
    void motoboyAcimaDoPesoMaximoEhIndisponivel() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Caixa Pesada", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 6}
                    ],
                    "modalidadeEntrega": "MOTOBOY",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("MODALIDADE_INDISPONIVEL")));
    }

    @Test
    void cupomMenos50AbaixoDoMinimoEhNaoAplicavel() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "RETIRADA_LOJA",
                    "cupom": "MENOS50",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("CUPOM_NAO_APLICAVEL")));
    }

    @Test
    void boletoAcimaDeMilEhIndisponivel() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Jaqueta de Couro", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 1}
                    ],
                    "modalidadeEntrega": "RETIRADA_LOJA",
                    "formaPagamento": "BOLETO",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("FORMA_PAGAMENTO_INDISPONIVEL")));
    }

    @Test
    void parcelamentoInvalidoParaPix() throws Exception {
        String corpo = """
                {
                    %s,
                    "modalidadeEntrega": "RETIRADA_LOJA",
                    "formaPagamento": "PIX",
                    "parcelas": 2,
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """.formatted(ITENS_PADRAO);

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PARCELAMENTO_INVALIDO")));
    }

    @Test
    void ouroComSubtotalAltoGanhaBrinde() throws Exception {
        String corpo = """
                {
                    "itens": [
                        {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 3, "pesoKg": 1.20}
                    ],
                    "modalidadeEntrega": "RETIRADA_LOJA",
                    "formaPagamento": "PIX",
                    "nivelClube": "OURO",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde", is(true)));
    }
}
