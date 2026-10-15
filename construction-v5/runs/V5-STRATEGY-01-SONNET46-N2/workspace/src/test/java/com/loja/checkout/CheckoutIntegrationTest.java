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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CheckoutIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // --------------- Exemplo 1 ---------------
    // Camiseta 79.90×2 (0.30kg) + Tênis 249.90×1 (1.20kg)
    // EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
    // subtotal 409.70 · cupom 40.97 · frete 33.10 · prazo 2 · seguro 10.24
    // ajuste −20.60 · total 391.47 · 1× de 391.47 · crédito 0.00 · brinde false
    @Test
    void exemplo1() throws Exception {
        String body = """
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

    // --------------- Exemplo 2 ---------------
    // Mesmos itens, ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE
    // subtotal 409.70 · cupom 0.00 · frete 15.60 · prazo 7 · seguro 6.15
    // ajuste 30.55 · total 462.00 · 6x de 77.00 · crédito 8.19 · brinde false
    @Test
    void exemplo2() throws Exception {
        String body = """
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

    // --------------- Exemplo 3 ---------------
    // Fone 199.90×2 (0.25kg), MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
    // subtotal 399.80 · cupom 50.00 · frete 18.00 · prazo 0 · seguro 8.00
    // ajuste 3.49 · total 379.29 · 1x de 379.29 · crédito 0.00 · brinde false
    @Test
    void exemplo3() throws Exception {
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

    // --------------- Exemplo 4 ---------------
    // Meia 19.90×7 (0.10kg) + Camiseta 79.90×2 (0.30kg)
    // RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL
    // subtotal 299.10 · cupom 39.80 · frete 0.00 · prazo 1 · seguro 2.99
    // ajuste 0.00 · total 262.29 · 3x de 87.43 · crédito 5.98 · brinde false
    @Test
    void exemplo4() throws Exception {
        String body = """
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

    // --------------- Exemplo 5 ---------------
    // Camiseta 79.90×2 (0.30kg) + Tenis 249.90×1 (1.20kg)
    // EXPRESSA, sem cupom, PIX, OURO, SUDESTE
    // subtotal 409.70 · cupom 0.00 · frete 0.00 · prazo 2 · seguro 4.10
    // ajuste -20.69 · total 393.11 · 1x de 393.11 · crédito 20.48 · brinde false
    @Test
    void exemplo5() throws Exception {
        String body = """
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

    // --------------- Erro: PEDIDO_INVALIDO (itens vazios) ---------------
    @Test
    void erroPedidoInvalido_itensVazios() throws Exception {
        String body = """
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    // --------------- Erro: MODALIDADE_INDISPONIVEL (motoboy > 5 kg) ---------------
    @Test
    void erroModalidadeIndisponivel_motoboyPesoExcedido() throws Exception {
        String body = """
                {
                  "itens": [
                    {"nome": "Geladeira", "precoUnitario": 1500.00, "quantidade": 1, "pesoKg": 6.00}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    // --------------- Erro: CUPOM_NAO_APLICAVEL (MENOS50 com subtotal < 300) ---------------
    @Test
    void erroCupomNaoAplicavel() throws Exception {
        String body = """
                {
                  "itens": [
                    {"nome": "Meia", "precoUnitario": 19.90, "quantidade": 2, "pesoKg": 0.10}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    // --------------- Erro: PARCELAMENTO_INVALIDO (PIX com 2 parcelas) ---------------
    @Test
    void erroParcelamentoInvalido_pixComParcelas() throws Exception {
        String body = """
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    // --------------- Erro: FORMA_PAGAMENTO_INDISPONIVEL (boleto > 1000) ---------------
    @Test
    void erroFormaPagamentoIndisponivel_boletoAcimaDeLimite() throws Exception {
        String body = """
                {
                  "itens": [
                    {"nome": "TV", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 5.00}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "BOLETO",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
