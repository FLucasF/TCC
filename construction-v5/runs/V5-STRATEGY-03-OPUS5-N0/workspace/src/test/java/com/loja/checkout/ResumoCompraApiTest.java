package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Os exemplos conferidos pelo financeiro e as recusas do servico. */
@SpringBootTest
@AutoConfigureMockMvc
class ResumoCompraApiTest {

    private static final String CAMISETA = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}""";
    private static final String TENIS = """
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() throws Exception {
        String pedido = """
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}"""
                .formatted(CAMISETA, TENIS);

        assertResumo(pedido, """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() throws Exception {
        String pedido = """
                {"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"CARTAO","parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}"""
                .formatted(CAMISETA, TENIS);

        assertResumo(pedido, """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() throws Exception {
        String pedido = """
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""";

        assertResumo(pedido, """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() throws Exception {
        String pedido = """
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}""".formatted(CAMISETA);

        assertResumo(pedido, """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() throws Exception {
        String pedido = """
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA, TENIS);

        assertResumo(pedido, """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemploDoAnexo_ouro_com_cupom_tem_frete_zerado() throws Exception {
        String pedido = """
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}"""
                .formatted(CAMISETA, TENIS);

        assertResumo(pedido, """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,
                 "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void valores_em_dinheiro_sempre_com_duas_casas() throws Exception {
        String pedido = """
                {"itens":[{"nome":"Bone","precoUnitario":50.00,"quantidade":2,"pesoKg":0.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":1,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""";

        String corpo = responder(pedido, status().isOk());

        assertThat(corpo).contains("\"subtotalProdutos\":100.00")
                .contains("\"descontoCupom\":0.00")
                .contains("\"frete\":0.00")
                .contains("\"totalFinal\":101.00")
                .contains("\"creditoProximaCompra\":0.00");
    }

    @Test
    void ouro_acima_de_500_em_produtos_ganha_brinde() throws Exception {
        String pedido = """
                {"itens":[{"nome":"Jaqueta","precoUnitario":260.00,"quantidade":2,"pesoKg":0.90}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO",
                 "regiao":"SUL"}""";

        assertResumo(pedido, """
                {"subtotalProdutos":520.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":5.20,"ajustePagamento":-26.26,"totalFinal":498.94,"parcelas":1,
                 "valorParcela":498.94,"creditoProximaCompra":26.00,"brinde":true}""");
    }

    @Test
    void fretegratis_desconta_exatamente_o_valor_do_frete() throws Exception {
        String pedido = """
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS",
                 "formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUDESTE"}"""
                .formatted(CAMISETA, TENIS);

        assertResumo(pedido, """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":3.49,"totalFinal":417.29,"parcelas":1,
                 "valorParcela":417.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void carrinho_vazio_e_item_invalido_sao_pedido_invalido() throws Exception {
        assertErro("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""", "PEDIDO_INVALIDO");

        assertErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":0,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE",
                 "regiao":"SUDESTE"}""", "PEDIDO_INVALIDO");

        assertErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE",
                 "regiao":"SUDESTE"}""", "PEDIDO_INVALIDO");
    }

    @Test
    void nivel_clube_e_regiao_invalidos() throws Exception {
        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"DIAMANTE","regiao":"SUDESTE"}""".formatted(CAMISETA),
                "NIVEL_CLUBE_INVALIDO");

        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "regiao":"SUDESTE"}""".formatted(CAMISETA), "NIVEL_CLUBE_INVALIDO");

        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"LESTE"}""".formatted(CAMISETA), "REGIAO_INVALIDA");
    }

    @Test
    void modalidade_invalida_e_indisponivel() throws Exception {
        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA),
                "MODALIDADE_INVALIDA");

        assertErro("""
                {"itens":[{"nome":"Mala","precoUnitario":300.00,"quantidade":2,"pesoKg":3.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE",
                 "regiao":"SUDESTE"}""", "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupom_invalido_e_nao_aplicavel() throws Exception {
        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"PROMO999","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA), "CUPOM_INVALIDO");

        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA), "CUPOM_INVALIDO");

        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA),
                "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void forma_de_pagamento_invalida_parcelamento_e_limite_do_boleto() throws Exception {
        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CRIPTO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA),
                "FORMA_PAGAMENTO_INVALIDA");

        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA),
                "PARCELAMENTO_INVALIDO");

        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA),
                "PARCELAMENTO_INVALIDO");

        assertErro("""
                {"itens":[{"nome":"Sofa","precoUnitario":600.00,"quantidade":2,"pesoKg":2.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","nivelClube":"BRONZE",
                 "regiao":"SUDESTE"}""", "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void a_ordem_de_conferencia_e_respeitada() throws Exception {
        // Tudo errado de uma vez: vale o primeiro problema da lista.
        assertErro("""
                {"itens":[],"modalidadeEntrega":"DRONE","cupom":"PROMO999","formaPagamento":"CRIPTO",
                 "nivelClube":"DIAMANTE","regiao":"LESTE"}""", "PEDIDO_INVALIDO");

        // Carrinho ok: a modalidade inexistente vem antes do cupom inexistente.
        assertErro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","cupom":"PROMO999","formaPagamento":"CRIPTO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA),
                "MODALIDADE_INVALIDA");
    }

    private void assertResumo(String pedido, String resumoEsperado) throws Exception {
        String corpo = responder(pedido, status().isOk());
        assertJson(resumoEsperado, corpo);
    }

    private void assertErro(String pedido, String codigoEsperado) throws Exception {
        String corpo = responder(pedido, status().isBadRequest());
        assertJson("{\"erro\":\"%s\"}".formatted(codigoEsperado), corpo);
    }

    private String responder(String pedido, org.springframework.test.web.servlet.ResultMatcher statusEsperado)
            throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andExpect(statusEsperado)
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void assertJson(String esperado, String obtido) throws JSONException {
        JSONAssert.assertEquals(esperado, obtido, JSONCompareMode.STRICT);
    }
}
