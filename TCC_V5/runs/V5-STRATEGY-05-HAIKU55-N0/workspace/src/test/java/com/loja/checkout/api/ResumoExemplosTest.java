package com.loja.checkout.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ResumoExemplosTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void exemplo1PixComCupomBemVindoEntregaExpressaRegiaoNorte() throws Exception {
        enviar(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"))
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
    void exemplo2CartaoSeisVezesComJurosRegiaoCentroOeste() throws Exception {
        enviar(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "ECONOMICA", null, "CARTAO", "6", "PRATA", "CENTRO_OESTE"))
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
    void exemplo3BoletoComCupomMenos50MotoboyRegiaoNordeste() throws Exception {
        String itens = """
                [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}]
                """;
        enviar(PedidoJson.montar(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.seguro").value(8.00))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(379.29))
                .andExpect(jsonPath("$.valorParcela").value(379.29))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo4CartaoTresVezesSemJurosLeve3Pague2RetiradaRegiaoSul() throws Exception {
        String itens = """
                [
                  {"nome": "Meia", "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10},
                  {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}
                ]
                """;
        enviar(PedidoJson.montar(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", "3", "PRATA", "SUL"))
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
    void exemplo5PixOuroSemFreteSudeste() throws Exception {
        enviar(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(jsonPath("$.totalFinal").value(393.11))
                .andExpect(jsonPath("$.valorParcela").value(393.11))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void ouroComSubtotalAcimaDe500GanhaBrinde() throws Exception {
        String itens = """
                [{"nome": "Bolsa", "precoUnitario": 260.00, "quantidade": 2, "pesoKg": 0.80}]
                """;
        enviar(PedidoJson.montar(itens, "ECONOMICA", null, "PIX", null, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(26.00))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
