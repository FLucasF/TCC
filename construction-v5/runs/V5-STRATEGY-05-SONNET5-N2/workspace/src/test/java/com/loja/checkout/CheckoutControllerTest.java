package com.loja.checkout;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressaComBemvindo10Pix() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
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
    void exemplo2_economicaCartao6xPrata() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
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
    void exemplo3_motoboyBoletoMenos50() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
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
    void exemplo4_retiradaLojaLeve3Pague2Prata() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
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
    void exemplo5_expressaOuroPixSemCupom() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
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
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [],
                                  "modalidadeEntrega": "ECONOMICA",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void motoboyAcimaDoPesoRetornaModalidadeIndisponivel() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome": "Caixa pesada", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 6.00}
                                  ],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("MODALIDADE_INDISPONIVEL")));
    }

    @Test
    void boletoAcimaDeMilRetornaFormaPagamentoIndisponivel() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome": "Produto caro", "precoUnitario": 1500.00, "quantidade": 1, "pesoKg": 1.00}
                                  ],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "formaPagamento": "BOLETO",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("FORMA_PAGAMENTO_INDISPONIVEL")));
    }

    @Test
    void parcelamentoInvalidoParaPix() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome": "Produto", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 1.00}
                                  ],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "formaPagamento": "PIX",
                                  "parcelas": 2,
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PARCELAMENTO_INVALIDO")));
    }

    @Test
    void cupomNaoAplicavelQuandoAbaixoDoMinimo() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome": "Produto", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 1.00}
                                  ],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "cupom": "MENOS50",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("CUPOM_NAO_APLICAVEL")));
    }
}
