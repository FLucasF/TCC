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
class ResumoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        String json = """
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

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
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
        String json = """
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

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
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
        String json = """
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

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
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
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() throws Exception {
        String json = """
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

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
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
        String json = """
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

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
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
    void carrinhoVazio_retornaPedidoInvalido() throws Exception {
        String json = """
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivelClubeInvalido_retornaErro() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "DIAMANTE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void regiaoInvalida_retornaErro() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "LESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void modalidadeInvalida_retornaErro() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboyAcimaDe5kg_retornaModalidadeIndisponivel() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": 10, "pesoKg": 1.0}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomInvalido_retornaErro() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "DESCONTO99",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void cupomNaoAplicavel_menos50AbaixoDe300() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void formaPagamentoInvalida_retornaErro() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "BITCOIN",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void parcelamentoInvalido_pixEm3x() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "parcelas": 3,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void cupomFreteGratis_descontoIgualAoFrete() throws Exception {
        String json = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "FRETEGRATIS",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(159.80))
                .andExpect(jsonPath("$.frete").value(13.20))
                .andExpect(jsonPath("$.descontoCupom").value(13.20))
                .andExpect(jsonPath("$.seguro").value(1.60))
                .andExpect(jsonPath("$.totalFinal").value(153.33));
    }

    @Test
    void ouroComSubtotalAlto_brindeTrue() throws Exception {
        String json = """
                {
                  "itens": [
                    {"nome": "Casaco", "precoUnitario": 510.00, "quantidade": 1, "pesoKg": 0.80}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(25.50));
    }

    @Test
    void itemComPrecoZero_retornaPedidoInvalido() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 0, "quantidade": 1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void itemComQuantidadeNegativa_retornaPedidoInvalido() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 10.00, "quantidade": -1, "pesoKg": 0.5}],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void boletoAcimaDe1000_retornaFormaPagamentoIndisponivel() throws Exception {
        String json = """
                {
                  "itens": [{"nome": "X", "precoUnitario": 500.00, "quantidade": 3, "pesoKg": 0.5}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
