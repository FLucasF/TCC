package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Os limites exatos que o enunciado define. */
@SpringBootTest
@AutoConfigureMockMvc
class FronteirasTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void menos50_com_exatamente_trezentos_reais_em_produtos_vale() throws Exception {
        calcular("""
                {"itens":[{"nome":"Calca","precoUnitario":150.00,"quantidade":2,"pesoKg":0.40}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(50.00));
    }

    @Test
    void menos50_um_centavo_abaixo_de_trezentos_nao_vale() throws Exception {
        calcular("""
                {"itens":[{"nome":"Calca","precoUnitario":299.99,"quantidade":1,"pesoKg":0.40}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void ouro_com_exatamente_quinhentos_reais_nao_leva_brinde() throws Exception {
        calcular("""
                {"itens":[{"nome":"Casaco","precoUnitario":250.00,"quantidade":2,"pesoKg":0.80}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void ouro_um_centavo_acima_de_quinhentos_leva_brinde() throws Exception {
        calcular("""
                {"itens":[{"nome":"Casaco","precoUnitario":500.01,"quantidade":1,"pesoKg":0.80}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void boleto_com_total_de_exatamente_mil_reais_e_aceito() throws Exception {
        // produtos 990,10 + seguro 9,90 (1% SUDESTE) = 1.000,00 de total do pedido
        calcular("""
                {"itens":[{"nome":"Mesa","precoUnitario":990.10,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
    }

    @Test
    void boleto_um_centavo_acima_de_mil_reais_nao_e_aceito() throws Exception {
        calcular("""
                {"itens":[{"nome":"Mesa","precoUnitario":990.11,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void cartao_em_quatro_vezes_ja_tem_juros() throws Exception {
        // total do pedido 101,00; Price 1,99% em 4x
        calcular("""
                {"itens":[{"nome":"Bone","precoUnitario":100.00,"quantidade":1,"pesoKg":0.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":4,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorParcela").value(26.52))
                .andExpect(jsonPath("$.totalFinal").value(106.08))
                .andExpect(jsonPath("$.ajustePagamento").value(5.08));
    }

    @Test
    void clube_ausente() throws Exception {
        calcular("""
                {"itens":[{"nome":"Bone","precoUnitario":100.00,"quantidade":1,"pesoKg":0.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void modalidade_ausente() throws Exception {
        calcular("""
                {"itens":[{"nome":"Bone","precoUnitario":100.00,"quantidade":1,"pesoKg":0.20}],
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void forma_de_pagamento_ausente() throws Exception {
        calcular("""
                {"itens":[{"nome":"Bone","precoUnitario":100.00,"quantidade":1,"pesoKg":0.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    private ResultActions calcular(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
