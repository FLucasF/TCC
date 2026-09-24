package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
        String corpo = """
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.09))
                .andExpect(jsonPath("$.totalFinal").value(381.74))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(381.74));
    }

    @Test
    void exemplo2_economicaSemCupomCartao6x() throws Exception {
        String corpo = """
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

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.ajustePagamento").value(30.10))
                .andExpect(jsonPath("$.totalFinal").value(455.40))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(75.90));
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
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(371.29))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(371.29));
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
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(259.30))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(86.43));
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void modalidadeInexistenteRetornaModalidadeInvalida() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Caixa pesada", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 6.00 }],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomInexistenteRetornaCupomInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "NAOEXISTE",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void formaPagamentoInexistenteRetornaFormaPagamentoInvalida() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CRIPTO"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void parcelamentoInvalidoNoPix() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 }],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "parcelas": 2
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() throws Exception {
        String corpo = """
                {
                  "itens": [{ "nome": "Notebook", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.00 }],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
