package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                        {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "BEMVINDO10",
                    "formaPagamento": "PIX",
                    "parcelas": 1,
                    "nivelClube": "BRONZE",
                    "regiao": "NORTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
        String body = """
                {
                    "itens": [
                        {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                        {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                    ],
                    "modalidadeEntrega": "ECONOMICA",
                    "formaPagamento": "CARTAO",
                    "parcelas": 6,
                    "nivelClube": "PRATA",
                    "regiao": "CENTRO_OESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
        String body = """
                {
                    "itens": [
                        {"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}
                    ],
                    "modalidadeEntrega": "MOTOBOY",
                    "cupom": "MENOS50",
                    "formaPagamento": "BOLETO",
                    "parcelas": 1,
                    "nivelClube": "BRONZE",
                    "regiao": "NORDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
    void exemplo4_retiradaLojaLeve3pague2Cartao3xPrataSul() throws Exception {
        String body = """
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

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
        String body = """
                {
                    "itens": [
                        {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                        {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "OURO",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
    void carrinhoVazio_devolvePedidoInvalido() throws Exception {
        String body = """
                {
                    "itens": [],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void itemComPrecoZero_devolvePedidoInvalido() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 0, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivelClubeInexistente_devolveNivelClubeInvalido() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "DIAMANTE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void regiaoInexistente_devolveRegiaoInvalida() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "EXTERIOR"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void modalidadeInexistente_devolveModalidadeInvalida() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "DRONE",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboyAcimaDe5kg_devolveModalidadeIndisponivel() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 6.0}],
                    "modalidadeEntrega": "MOTOBOY",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomInexistente_devolveCupomInvalido() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "DESCONTO99",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50AbaixoDoMinimo_devolveCupomNaoAplicavel() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 100, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "MENOS50",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void formaPagamentoInexistente_devolveFormaPagamentoInvalida() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "CRIPTO",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void pixComParcelas_devolveParcelamentoInvalido() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "parcelas": 3,
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void freteGratis_descontoIgualAoFrete() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                        {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "FRETEGRATIS",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void ouroComBrinde_subtotalAcimaDe500() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome": "Jaqueta", "precoUnitario": 510.00, "quantidade": 1, "pesoKg": 1.00}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "CARTAO",
                    "parcelas": 1,
                    "nivelClube": "OURO",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(510.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(25.50))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void cartao12xComJuros() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome": "Tênis", "precoUnitario": 300.00, "quantidade": 1, "pesoKg": 1.00}
                    ],
                    "modalidadeEntrega": "RETIRADA_LOJA",
                    "formaPagamento": "CARTAO",
                    "parcelas": 12,
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(300.00))
                .andExpect(jsonPath("$.parcelas").value(12));
    }

    @Test
    void boletoAcimaDe1000_devolveFormaPagamentoIndisponivel() throws Exception {
        String body = """
                {
                    "itens": [{"nome": "X", "precoUnitario": 1000, "quantidade": 2, "pesoKg": 0.5}],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "BOLETO",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
