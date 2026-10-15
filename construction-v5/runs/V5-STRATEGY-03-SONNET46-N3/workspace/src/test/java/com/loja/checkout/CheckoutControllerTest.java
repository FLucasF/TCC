package com.loja.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CheckoutControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private static final String URL = "/checkout/resumo";

    // -------------------------------------------------------------------------
    // Exemplos do financeiro
    // -------------------------------------------------------------------------

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                        {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                    ],
                    "modalidadeEntrega":"EXPRESSA",
                    "cupom":"BEMVINDO10",
                    "formaPagamento":"PIX",
                    "nivelClube":"BRONZE",
                    "regiao":"NORTE"
                }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
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
    void exemplo2_economica_semcupom_cartao6x_prata_centrooeste() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                        {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                    ],
                    "modalidadeEntrega":"ECONOMICA",
                    "formaPagamento":"CARTAO",
                    "parcelas":6,
                    "nivelClube":"PRATA",
                    "regiao":"CENTRO_OESTE"
                }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
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
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}
                    ],
                    "modalidadeEntrega":"MOTOBOY",
                    "cupom":"MENOS50",
                    "formaPagamento":"BOLETO",
                    "nivelClube":"BRONZE",
                    "regiao":"NORDESTE"
                }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
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
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                        {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}
                    ],
                    "modalidadeEntrega":"RETIRADA_LOJA",
                    "cupom":"LEVE3PAGUE2",
                    "formaPagamento":"CARTAO",
                    "parcelas":3,
                    "nivelClube":"PRATA",
                    "regiao":"SUL"
                }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
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
    void exemplo5_expressa_semcupom_pix_ouro_sudeste() throws Exception {
        String body = """
                {
                    "itens": [
                        {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                        {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                    ],
                    "modalidadeEntrega":"EXPRESSA",
                    "formaPagamento":"PIX",
                    "nivelClube":"OURO",
                    "regiao":"SUDESTE"
                }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
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

    // -------------------------------------------------------------------------
    // Erros de validação
    // -------------------------------------------------------------------------

    @Test
    void erro_carrinho_vazio() throws Exception {
        String body = """
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erro_item_preco_zero() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erro_nivel_clube_invalido() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"DIAMANTE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void erro_regiao_invalida() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"EUROPA"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void erro_modalidade_invalida() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"DRONE","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void erro_motoboy_acima_5kg() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":5.01}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void erro_cupom_invalido() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"FALSO","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void erro_menos50_abaixo_do_minimo() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void erro_forma_pagamento_invalida() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"CRIPTOMOEDA",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void erro_pix_parcelado() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erro_boleto_acima_de_1000() throws Exception {
        String body = """
                {"itens":[{"nome":"X","precoUnitario":500,"quantidade":3,"pesoKg":0.5}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    // -------------------------------------------------------------------------
    // Cobertura de comportamentos específicos não presentes nos exemplos
    // -------------------------------------------------------------------------

    @Test
    void fretegratis_desconto_igual_ao_frete() throws Exception {
        // subtotal=599.90 · frete ECONOMICA(2kg)=16.00 · descontoCupom=16.00
        // seguro SUDESTE 1%=6.00 · total=605.90 · PIX 5%=30.30 · totalFinal=575.60
        String body = """
                {
                    "itens": [
                        {"nome":"Notebook","precoUnitario":599.90,"quantidade":1,"pesoKg":2.00}
                    ],
                    "modalidadeEntrega":"ECONOMICA",
                    "cupom":"FRETEGRATIS",
                    "formaPagamento":"PIX",
                    "nivelClube":"BRONZE",
                    "regiao":"SUDESTE"
                }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(16.00))
                .andExpect(jsonPath("$.descontoCupom").value(16.00))
                .andExpect(jsonPath("$.seguro").value(6.00))
                .andExpect(jsonPath("$.ajustePagamento").value(-30.30))
                .andExpect(jsonPath("$.totalFinal").value(575.60));
    }

    @Test
    void ouro_brinde_quando_subtotal_acima_de_500() throws Exception {
        // subtotal=599.90 · OURO: frete=0 · seguro SUDESTE 1%=6.00
        // total=605.90 · CARTAO 1x: sem juros · crédito 5%=30.00 · brinde=true
        String body = """
                {
                    "itens": [
                        {"nome":"TV","precoUnitario":599.90,"quantidade":1,"pesoKg":5.00}
                    ],
                    "modalidadeEntrega":"RETIRADA_LOJA",
                    "formaPagamento":"CARTAO",
                    "parcelas":1,
                    "nivelClube":"OURO",
                    "regiao":"SUDESTE"
                }
                """;
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(605.90))
                .andExpect(jsonPath("$.creditoProximaCompra").value(30.00))
                .andExpect(jsonPath("$.brinde").value(true));
    }
}
