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
class CheckoutTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() throws Exception {
        String req = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis",    "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
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
        String req = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis",    "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6,
                  "nivelClube": "PRATA",
                  "regiao": "CENTRO_OESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
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
        String req = """
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
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
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
        String req = """
                {
                  "itens": [
                    {"nome": "Meia",     "precoUnitario": 19.90, "quantidade": 7, "pesoKg": 0.10},
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
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
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
        String req = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis",    "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
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
    void erroCarrinhoVazio() throws Exception {
        String req = """
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erroModalidadeIndisponivel_motoboyAcimaDe5kg() throws Exception {
        String req = """
                {
                  "itens": [
                    {"nome": "Caixa", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 6.00}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void erroCupomInvalido() throws Exception {
        String req = """
                {
                  "itens": [
                    {"nome": "Item", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.50}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "CUPOMINEXISTENTE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void erroCupomNaoAplicavel_menos50abaixoDe300() throws Exception {
        String req = """
                {
                  "itens": [
                    {"nome": "Item", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.50}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void erroBoletoPedidoAcimaDe1000() throws Exception {
        String req = """
                {
                  "itens": [
                    {"nome": "Item", "precoUnitario": 600.00, "quantidade": 2, "pesoKg": 1.00}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void erroParcelamentoInvalidoPixComMaisDe1Parcela() throws Exception {
        String req = """
                {
                  "itens": [
                    {"nome": "Item", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 0.50}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }
}
