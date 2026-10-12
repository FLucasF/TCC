package com.loja.checkout;

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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoDoCheckoutTest {

    private static final String CAMISETA = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}""";
    private static final String TENIS = """
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() throws Exception {
        calcular("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA, TENIS))
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
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() throws Exception {
        calcular("""
                {"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"CARTAO","parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA, TENIS))
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
        calcular("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}
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

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() throws Exception {
        calcular("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2",
                 "formaPagamento":"CARTAO","parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
                """.formatted(CAMISETA))
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
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() throws Exception {
        calcular("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA, TENIS))
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
    void exemplo_do_anexo_ouro_com_cupom() throws Exception {
        calcular("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-18.64))
                .andExpect(jsonPath("$.totalFinal").value(354.19))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48));
    }

    @Test
    void valores_de_dinheiro_saem_com_duas_casas() throws Exception {
        calcular("""
                {"itens":[{"nome":"Boné","precoUnitario":100.00,"quantidade":1,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":1,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":101.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")));
    }

    @Test
    void frete_gratis_desconta_o_frete_mostrado() throws Exception {
        calcular("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS",
                 "formaPagamento":"CARTAO","parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }

    @Test
    void ouro_com_frete_gratis_nao_desconta_nada() throws Exception {
        calcular("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS",
                 "formaPagamento":"CARTAO","parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.descontoCupom").value(0.00));
    }

    @Test
    void ouro_acima_de_500_em_produtos_leva_brinde() throws Exception {
        calcular("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":300.00,"quantidade":2,"pesoKg":0.80}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true))
                .andExpect(jsonPath("$.creditoProximaCompra").value(30.00));
    }

    private ResultActions calcular(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
