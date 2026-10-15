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

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
    private static final String TENIS = "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";
    private static final String FONE = "{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}";
    private static final String MEIA = "{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10}";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String pedido(String itens, String modalidade, String cupom, String pagamento, Integer parcelas,
                          String nivel, String regiao) {
        return """
                {"itens":[%s],"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,"parcelas":%s,
                 "nivelClube":%s,"regiao":%s}
                """.formatted(itens, json(modalidade), json(cupom), json(pagamento),
                parcelas == null ? "null" : parcelas.toString(), json(nivel), json(regiao));
    }

    private String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix() throws Exception {
        resumo(pedido(CAMISETA + "," + TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.imposto").value(44.25))
                .andExpect(jsonPath("$.ajustePagamento").value(-22.30))
                .andExpect(jsonPath("$.totalFinal").value(423.78))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(423.78))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() throws Exception {
        resumo(pedido(CAMISETA + "," + TENIS, "ECONOMICA", null, "CARTAO", 6, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.imposto").value(49.16))
                .andExpect(jsonPath("$.ajustePagamento").value(33.56))
                .andExpect(jsonPath("$.totalFinal").value(508.02))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(84.67));
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() throws Exception {
        resumo(pedido(FONE, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.imposto").value(41.98))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(413.27))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(413.27));
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_sem_juros() throws Exception {
        resumo(pedido(MEIA + "," + CAMISETA, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.imposto").value(31.12))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(290.42))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(96.81));
    }

    @Test
    void exemplo5_ouro_nao_paga_frete_e_ganha_credito() throws Exception {
        resumo(pedido(CAMISETA + "," + TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.imposto").value(49.16))
                .andExpect(jsonPath("$.ajustePagamento").value(-22.94))
                .andExpect(jsonPath("$.totalFinal").value(435.92))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void prata_ganha_dois_porcento_de_credito() throws Exception {
        resumo(pedido(CAMISETA + "," + TENIS, "RETIRADA_LOJA", null, "PIX", 1, "PRATA", "NORTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imposto").value(28.68))
                .andExpect(jsonPath("$.creditoProximaCompra").value(8.19))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() throws Exception {
        resumo(pedido(TENIS.replace("\"quantidade\":1", "\"quantidade\":3"), "MOTOBOY", null, "PIX", 1, "OURO",
                "NORDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(749.70))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(37.48))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void fretegratis_zera_o_frete_no_desconto() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(159.80))
                .andExpect(jsonPath("$.frete").value(27.70))
                .andExpect(jsonPath("$.descontoCupom").value(27.70))
                .andExpect(jsonPath("$.imposto").value(14.53));
    }

    @Test
    void imposto_usa_a_aliquota_da_regiao() throws Exception {
        resumo(pedido(CAMISETA, "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "CENTRO_OESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imposto").value(14.38));
    }

    @Test
    void carrinho_vazio_e_pedido_invalido() throws Exception {
        resumo(pedido("", "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void item_com_quantidade_zero_e_pedido_invalido() throws Exception {
        resumo(pedido(CAMISETA.replace("\"quantidade\":2", "\"quantidade\":0"), "EXPRESSA", null, "PIX", 1, "BRONZE",
                "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void item_sem_peso_e_pedido_invalido() throws Exception {
        resumo(pedido("{\"nome\":\"X\",\"precoUnitario\":10.00,\"quantidade\":1}", "EXPRESSA", null, "PIX", 1,
                "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivel_de_clube_inexistente() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void regiao_ausente() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", null, "PIX", 1, "BRONZE", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void modalidade_inexistente() throws Exception {
        resumo(pedido(CAMISETA, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboy_acima_de_cinco_quilos_fica_indisponivel() throws Exception {
        resumo(pedido(TENIS.replace("\"quantidade\":1", "\"quantidade\":5"), "MOTOBOY", null, "PIX", 1, "BRONZE",
                "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupom_inexistente() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50_abaixo_do_minimo_nao_se_aplica() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void forma_de_pagamento_inexistente() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", null, "DINHEIRO", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void pix_nao_parcela() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void cartao_acima_de_doze_parcelas() throws Exception {
        resumo(pedido(CAMISETA, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void boleto_acima_de_mil_reais_fica_indisponivel() throws Exception {
        resumo(pedido(TENIS.replace("\"quantidade\":1", "\"quantidade\":4"), "ECONOMICA", null, "BOLETO", 1, "BRONZE",
                "SUDESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
