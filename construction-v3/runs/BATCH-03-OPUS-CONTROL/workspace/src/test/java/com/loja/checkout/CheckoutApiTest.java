package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
    private static final String TENIS = "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions chamar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix() throws Exception {
        chamar("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.09))
                .andExpect(jsonPath("$.totalFinal").value(381.74))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(381.74))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("409.70")));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() throws Exception {
        chamar("""
                {"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6}
                """.formatted(CAMISETA, TENIS))
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
    void exemplo3_motoboy_menos50_boleto() throws Exception {
        chamar("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO"}
                """)
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
    void exemplo4_retirada_leve3pague2_cartao_3x() throws Exception {
        chamar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3}
                """.formatted(CAMISETA))
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
    void fretegratis_zera_o_frete_no_total() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO","parcelas":1}
                """.formatted(TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(249.90))
                .andExpect(jsonPath("$.frete").value(30.40))
                .andExpect(jsonPath("$.descontoCupom").value(30.40))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(249.90));
    }

    @Test
    void carrinho_vazio_e_pedido_invalido() throws Exception {
        chamar("{\"itens\":[],\"modalidadeEntrega\":\"XPTO\",\"formaPagamento\":\"CHEQUE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void item_com_quantidade_zero_e_pedido_invalido() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":0,\"pesoKg\":0.10}],"
                + "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void item_sem_peso_e_pedido_invalido() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":1}],"
                + "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void modalidade_inexistente() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"DRONE\",\"cupom\":\"NAOEXISTE\",\"formaPagamento\":\"CHEQUE\"}"
                .formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void modalidade_nao_informada() throws Exception {
        chamar("{\"itens\":[%s],\"formaPagamento\":\"PIX\"}".formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboy_acima_de_5kg_fica_indisponivel() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Bota\",\"precoUnitario\":100.00,\"quantidade\":4,\"pesoKg\":1.50}],"
                + "\"modalidadeEntrega\":\"MOTOBOY\",\"cupom\":\"NAOEXISTE\",\"formaPagamento\":\"PIX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void motoboy_com_exatamente_5kg_e_aceito() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Bota\",\"precoUnitario\":100.00,\"quantidade\":2,\"pesoKg\":2.50}],"
                + "\"modalidadeEntrega\":\"MOTOBOY\",\"formaPagamento\":\"PIX\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.totalFinal").value(207.10));
    }

    @Test
    void cupom_inexistente() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"ECONOMICA\",\"cupom\":\"PROMO99\",\"formaPagamento\":\"CHEQUE\"}"
                .formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void cupom_em_minusculas_nao_existe() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"ECONOMICA\",\"cupom\":\"bemvindo10\",\"formaPagamento\":\"PIX\"}"
                .formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50_abaixo_do_minimo_nao_e_aplicavel() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":2,\"pesoKg\":0.10}],"
                + "\"modalidadeEntrega\":\"ECONOMICA\",\"cupom\":\"MENOS50\",\"formaPagamento\":\"CHEQUE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void menos50_com_exatamente_300_em_produtos_e_aplicavel() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Kit\",\"precoUnitario\":150.00,\"quantidade\":2,\"pesoKg\":0.50}],"
                + "\"modalidadeEntrega\":\"RETIRADA_LOJA\",\"cupom\":\"MENOS50\",\"formaPagamento\":\"CARTAO\",\"parcelas\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.totalFinal").value(250.00));
    }

    @Test
    void forma_de_pagamento_inexistente() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CHEQUE\",\"parcelas\":99}"
                .formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void forma_de_pagamento_nao_informada() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"ECONOMICA\"}".formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void pix_parcelado_nao_e_permitido() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\",\"parcelas\":2}"
                .formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void boleto_parcelado_nao_e_permitido() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"BOLETO\",\"parcelas\":3}"
                .formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void cartao_em_13x_nao_e_permitido() throws Exception {
        chamar("{\"itens\":[%s],\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CARTAO\",\"parcelas\":13}"
                .formatted(TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void boleto_acima_de_mil_reais_fica_indisponivel() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Jaqueta\",\"precoUnitario\":500.00,\"quantidade\":2,\"pesoKg\":1.00}],"
                + "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"BOLETO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void boleto_com_exatamente_mil_reais_e_aceito() throws Exception {
        chamar("{\"itens\":[{\"nome\":\"Jaqueta\",\"precoUnitario\":500.00,\"quantidade\":2,\"pesoKg\":1.00}],"
                + "\"modalidadeEntrega\":\"RETIRADA_LOJA\",\"formaPagamento\":\"BOLETO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
    }

    @Test
    void corpo_fora_do_formato_e_pedido_invalido() throws Exception {
        chamar("{\"itens\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
