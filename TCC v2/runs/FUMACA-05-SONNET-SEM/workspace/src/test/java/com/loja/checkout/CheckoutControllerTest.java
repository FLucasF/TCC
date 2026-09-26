package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressaComBemvindo10NoPix() throws Exception {
        String body = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
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
        String body = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
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
        String body = """
                {
                  "itens": [
                    { "nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25 }
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
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
    void exemplo4_retiradaLojaComLeve3Pague2Cartao3x() throws Exception {
        String body = """
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

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
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
        String body = """
                {
                  "itens": [],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void itemComPrecoZeroRetornaPedidoInvalido() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 0, "quantidade": 1, "pesoKg": 0.3 } ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PEDIDO_INVALIDO")));
    }

    @Test
    void modalidadeInexistenteRetornaModalidadeInvalida() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.3 } ],
                  "modalidadeEntrega": "TELEPORTE",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("MODALIDADE_INVALIDA")));
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Sofa", "precoUnitario": 500, "quantidade": 1, "pesoKg": 6 } ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("MODALIDADE_INDISPONIVEL")));
    }

    @Test
    void cupomInexistenteRetornaCupomInvalido() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.3 } ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "NAOEXISTE",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("CUPOM_INVALIDO")));
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 50, "quantidade": 1, "pesoKg": 0.3 } ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("CUPOM_NAO_APLICAVEL")));
    }

    @Test
    void formaPagamentoInexistenteRetornaFormaInvalida() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.3 } ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "DINHEIRO"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("FORMA_PAGAMENTO_INVALIDA")));
    }

    @Test
    void pixComParcelamentoRetornaParcelamentoInvalido() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.3 } ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "parcelas": 2
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PARCELAMENTO_INVALIDO")));
    }

    @Test
    void cartaoComTrezePercelasRetornaParcelamentoInvalido() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.3 } ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 13
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("PARCELAMENTO_INVALIDO")));
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() throws Exception {
        String body = """
                {
                  "itens": [ { "nome": "Notebook", "precoUnitario": 1200, "quantidade": 1, "pesoKg": 2 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", is("FORMA_PAGAMENTO_INDISPONIVEL")));
    }
}
