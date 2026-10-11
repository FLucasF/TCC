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
class CheckoutControllerTest {

    @Autowired
    MockMvc mvc;

    // ── Exemplos conferidos pelo financeiro ──────────────────────────────────

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                                    {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                                  ],
                                  "modalidadeEntrega":"EXPRESSA",
                                  "cupom":"BEMVINDO10",
                                  "formaPagamento":"PIX",
                                  "parcelas":1,
                                  "nivelClube":"BRONZE",
                                  "regiao":"NORTE"
                                }
                                """))
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
    void exemplo2_economica_semCupom_cartao6x_prata_centroOeste() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                                    {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                                  ],
                                  "modalidadeEntrega":"ECONOMICA",
                                  "formaPagamento":"CARTAO",
                                  "parcelas":6,
                                  "nivelClube":"PRATA",
                                  "regiao":"CENTRO_OESTE"
                                }
                                """))
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
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
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
    void exemplo4_retiradaLoja_leve3pague2_cartao3x_prata_sul() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
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
    void exemplo5_expressa_semCupom_pix_ouro_sudeste() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                                    {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
                                  ],
                                  "modalidadeEntrega":"EXPRESSA",
                                  "formaPagamento":"PIX",
                                  "nivelClube":"OURO",
                                  "regiao":"SUDESTE"
                                }
                                """))
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
    void fretegratis_desconto_igual_ao_frete() throws Exception {
        // Camiseta 79.90×2, peso 0.90/item → pesoTotal 1.80 kg
        // EXPRESSA: frete = 25 + 4.50×1.80 = 33.10
        // FRETEGRATIS: descontoCupom = frete = 33.10
        // seguro SUDESTE: 1% × 159.80 = 1.598 → 1.60
        // totalBase = 159.80 - 33.10 + 33.10 + 1.60 = 161.40
        // PIX: 5% × 161.40 = 8.07 → ajuste = -8.07, totalFinal = 153.33
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.90}
                                  ],
                                  "modalidadeEntrega":"EXPRESSA",
                                  "cupom":"FRETEGRATIS",
                                  "formaPagamento":"PIX",
                                  "nivelClube":"BRONZE",
                                  "regiao":"SUDESTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(159.80))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(1.60))
                .andExpect(jsonPath("$.ajustePagamento").value(-8.07))
                .andExpect(jsonPath("$.totalFinal").value(153.33))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    // ── Erros ────────────────────────────────────────────────────────────────

    @Test
    void erro_carrinhoVazio() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erro_itemComPrecoZero() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":0,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erro_nivelClubeInvalido() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                                 "nivelClube":"DIAMANTE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void erro_regiaoInvalida() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"EUROPA"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void erro_modalidadeInvalida() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"DRONE","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void erro_motoboyAcimaDoLimite() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":6}],
                                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void erro_cupomInvalido() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","cupom":"NAOEXISTE","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void erro_cupomMenos50NaoAplicavel() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void erro_formaPagamentoInvalida() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"CRIPTOMOEDA",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void erro_pixComParcelamento() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erro_boletoAcimaDoLimite() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itens":[{"nome":"X","precoUnitario":600,"quantidade":2,"pesoKg":0.5}],
                                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO",
                                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
