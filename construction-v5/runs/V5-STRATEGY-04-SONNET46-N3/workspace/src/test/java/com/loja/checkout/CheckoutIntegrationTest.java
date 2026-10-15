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
class CheckoutIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private org.springframework.test.web.servlet.ResultActions resumo(String body) throws Exception {
        return mvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // Exemplo 1: EXPRESSA + BEMVINDO10 + PIX + BRONZE + NORTE
    @Test
    void exemplo1() throws Exception {
        resumo("""
                {
                  "itens": [
                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                    {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """)
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

    // Exemplo 2: ECONOMICA + sem cupom + CARTAO 6x + PRATA + CENTRO_OESTE
    @Test
    void exemplo2() throws Exception {
        resumo("""
                {
                  "itens": [
                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                    {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6,
                  "nivelClube": "PRATA",
                  "regiao": "CENTRO_OESTE"
                }
                """)
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

    // Exemplo 3: MOTOBOY + MENOS50 + BOLETO + BRONZE + NORDESTE
    @Test
    void exemplo3() throws Exception {
        resumo("""
                {
                  "itens": [
                    {"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """)
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

    // Exemplo 4: RETIRADA_LOJA + LEVE3PAGUE2 + CARTAO 3x + PRATA + SUL
    @Test
    void exemplo4() throws Exception {
        resumo("""
                {
                  "itens": [
                    {"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": "LEVE3PAGUE2",
                  "formaPagamento": "CARTAO",
                  "parcelas": 3,
                  "nivelClube": "PRATA",
                  "regiao": "SUL"
                }
                """)
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

    // Exemplo 5: EXPRESSA + sem cupom + PIX + OURO + SUDESTE
    @Test
    void exemplo5() throws Exception {
        resumo("""
                {
                  "itens": [
                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                    {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """)
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

    // Erros de validação

    @Test
    void erroCarrinhoVazio() throws Exception {
        resumo("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":1,"nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erroNivelClubeInvalido() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.5}],
                  "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":1,
                  "nivelClube":"DIAMANTE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void erroModalidadeIndisponivel() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":6.0}],
                  "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","parcelas":1,
                  "nivelClube":"BRONZE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void erroCupomInvalido() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.5}],
                  "modalidadeEntrega":"EXPRESSA","cupom":"INVALIDO","formaPagamento":"PIX","parcelas":1,
                  "nivelClube":"BRONZE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void erroCupomNaoAplicavel() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.5}],
                  "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX","parcelas":1,
                  "nivelClube":"BRONZE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void erroParcelamentoInvalido() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.5}],
                  "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":3,
                  "nivelClube":"BRONZE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erroRegiaoInvalida() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.5}],
                  "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":1,
                  "nivelClube":"BRONZE","regiao":"PATAGONIA"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void erroModalidadeInvalida() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.5}],
                  "modalidadeEntrega":"DRONE","formaPagamento":"PIX","parcelas":1,
                  "nivelClube":"BRONZE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void erroFormaPagamentoInvalida() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.5}],
                  "modalidadeEntrega":"EXPRESSA","formaPagamento":"CRIPTO","parcelas":1,
                  "nivelClube":"BRONZE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void erroBoletoAcimaDoLimite() throws Exception {
        resumo("""
                {
                  "itens":[{"nome":"X","precoUnitario":600.00,"quantidade":2,"pesoKg":1.0}],
                  "modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","parcelas":1,
                  "nivelClube":"BRONZE","regiao":"SUL"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
