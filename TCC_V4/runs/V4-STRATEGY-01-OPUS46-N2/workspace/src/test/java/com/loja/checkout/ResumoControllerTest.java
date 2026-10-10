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
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                        {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "BEMVINDO10",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "NORTE"
                }
                """;
        String expected = """
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
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() throws Exception {
        String request = """
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
        String expected = """
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
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() throws Exception {
        String request = """
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
        String expected = """
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
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }

    @Test
    void exemplo4_retiradaLojaLeve3pague2Cartao3xPrataSul() throws Exception {
        String request = """
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
        String expected = """
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
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() throws Exception {
        String request = """
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
        String expected = """
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
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }

    @Test
    void carrinhoVazio() throws Exception {
        String request = """
                {
                    "itens": [],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void itemComPrecoZero() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 0, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivelClubeInvalido() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "DIAMANTE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void regiaoInvalida() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "EXTERIOR"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void modalidadeInvalida() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "DRONE",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboyAcimaDe5kg() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 100, "quantidade": 1, "pesoKg": 5.10}
                    ],
                    "modalidadeEntrega": "MOTOBOY",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomInvalido() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 100, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "DESCONTO99",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void cupomNaoAplicavel_menos50AbaixoDe300() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 100, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "cupom": "MENOS50",
                    "formaPagamento": "PIX",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void formaPagamentoInvalida() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 100, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "CRIPTO",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void parcelamentoInvalido_pixEm2x() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 100, "quantidade": 1, "pesoKg": 0.10}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "PIX",
                    "parcelas": 2,
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcimaDe1000() throws Exception {
        String request = """
                {
                    "itens": [
                        {"nome": "X", "precoUnitario": 500, "quantidade": 3, "pesoKg": 0.50}
                    ],
                    "modalidadeEntrega": "EXPRESSA",
                    "formaPagamento": "BOLETO",
                    "nivelClube": "BRONZE",
                    "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void ordemDeErros_pedidoInvalidoAntesDeClubeInvalido() throws Exception {
        String request = """
                {
                    "itens": [],
                    "modalidadeEntrega": "DRONE",
                    "formaPagamento": "CRIPTO",
                    "nivelClube": "DIAMANTE",
                    "regiao": "EXTERIOR"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
