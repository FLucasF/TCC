package com.loja.checkout.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressaComBemvindo10NoPix() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                    { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(409.70)))
                .andExpect(jsonPath("$.descontoCupom", is(40.97)))
                .andExpect(jsonPath("$.frete", is(33.10)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(2)))
                .andExpect(jsonPath("$.ajustePagamento", is(-20.09)))
                .andExpect(jsonPath("$.totalFinal", is(381.74)))
                .andExpect(jsonPath("$.parcelas", is(1)))
                .andExpect(jsonPath("$.valorParcela", is(381.74)));
    }

    @Test
    void exemplo2_economicaSemCupomCartao6x() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                    { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(409.70)))
                .andExpect(jsonPath("$.descontoCupom", is(0.00)))
                .andExpect(jsonPath("$.frete", is(15.60)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(7)))
                .andExpect(jsonPath("$.ajustePagamento", is(30.10)))
                .andExpect(jsonPath("$.totalFinal", is(455.40)))
                .andExpect(jsonPath("$.parcelas", is(6)))
                .andExpect(jsonPath("$.valorParcela", is(75.90)));
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoleto() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25 }
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO",
                  "parcelas": 1
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(399.80)))
                .andExpect(jsonPath("$.descontoCupom", is(50.00)))
                .andExpect(jsonPath("$.frete", is(18.00)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(0)))
                .andExpect(jsonPath("$.ajustePagamento", is(3.49)))
                .andExpect(jsonPath("$.totalFinal", is(371.29)))
                .andExpect(jsonPath("$.parcelas", is(1)))
                .andExpect(jsonPath("$.valorParcela", is(371.29)));
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2NoCartao3x() throws Exception {
        String corpo = """
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
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos", is(299.10)))
                .andExpect(jsonPath("$.descontoCupom", is(39.80)))
                .andExpect(jsonPath("$.frete", is(0.00)))
                .andExpect(jsonPath("$.prazoEntregaDias", is(1)))
                .andExpect(jsonPath("$.ajustePagamento", is(0.00)))
                .andExpect(jsonPath("$.totalFinal", is(259.30)))
                .andExpect(jsonPath("$.parcelas", is(3)))
                .andExpect(jsonPath("$.valorParcela", is(86.43)));
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 0, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void modalidadeInexistenteRetornaModalidadeInvalida() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "TELEPORTE",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("MODALIDADE_INVALIDA")));
    }

    @Test
    void motoboyAcimaDoLimiteDePesoRetornaModalidadeIndisponivel() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Halteres", "precoUnitario": 199.90, "quantidade": 3, "pesoKg": 2.5 }
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("MODALIDADE_INDISPONIVEL")));
    }

    @Test
    void cupomInexistenteRetornaCupomInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "NAOEXISTE",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("CUPOM_INVALIDO")));
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("CUPOM_NAO_APLICAVEL")));
    }

    @Test
    void formaPagamentoInexistenteRetornaFormaPagamentoInvalida() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "CHEQUE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("FORMA_PAGAMENTO_INVALIDA")));
    }

    @Test
    void pixComParcelamentoRetornaParcelamentoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "parcelas": 2
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PARCELAMENTO_INVALIDO")));
    }

    @Test
    void cartaoAcimaDe12ParcelasRetornaParcelamentoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 13
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PARCELAMENTO_INVALIDO")));
    }

    @Test
    void boletoAcimaDeMilRetornaFormaPagamentoIndisponivel() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Notebook", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.0 }
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("FORMA_PAGAMENTO_INDISPONIVEL")));
    }
}
