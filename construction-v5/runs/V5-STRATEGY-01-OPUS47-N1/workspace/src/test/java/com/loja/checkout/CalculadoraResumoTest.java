package com.loja.checkout;

import com.loja.checkout.servico.CalculadoraResumo;
import com.loja.checkout.servico.CheckoutErro;
import com.loja.checkout.servico.ResultadoCheckout;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResposta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CalculadoraResumoTest {

    private final CalculadoraResumo calc = new CalculadoraResumo();

    private static CheckoutRequest.ItemRequest item(String nome, String preco, int qtd, String peso) {
        CheckoutRequest.ItemRequest i = new CheckoutRequest.ItemRequest();
        i.nome = nome;
        i.precoUnitario = new BigDecimal(preco);
        i.quantidade = qtd;
        i.pesoKg = new BigDecimal(peso);
        return i;
    }

    private static CheckoutRequest req(List<CheckoutRequest.ItemRequest> itens, String modalidade,
                                       String cupom, String forma, Integer parcelas, String nivel, String regiao) {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = itens;
        r.modalidadeEntrega = modalidade;
        r.cupom = cupom;
        r.formaPagamento = forma;
        r.parcelas = parcelas;
        r.nivelClube = nivel;
        r.regiao = regiao;
        return r;
    }

    private CheckoutResposta sucesso(CheckoutRequest r) {
        ResultadoCheckout res = calc.calcular(r);
        return assertInstanceOf(ResultadoCheckout.Sucesso.class, res).resposta();
    }

    private CheckoutErro erro(CheckoutRequest r) {
        ResultadoCheckout res = calc.calcular(r);
        return assertInstanceOf(ResultadoCheckout.Falha.class, res).codigo();
    }

    private static void eq(String esperado, BigDecimal real) {
        assertEquals(new BigDecimal(esperado), real);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        var r = req(List.of(
            item("Camiseta", "79.90", 2, "0.30"),
            item("Tenis", "249.90", 1, "1.20")
        ), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        CheckoutResposta out = sucesso(r);
        eq("409.70", out.subtotalProdutos());
        eq("40.97", out.descontoCupom());
        eq("33.10", out.frete());
        assertEquals(2, out.prazoEntregaDias());
        eq("10.24", out.seguro());
        eq("-20.60", out.ajustePagamento());
        eq("391.47", out.totalFinal());
        assertEquals(1, out.parcelas());
        eq("391.47", out.valorParcela());
        eq("0.00", out.creditoProximaCompra());
        assertEquals(false, out.brinde());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centrooeste() {
        var r = req(List.of(
            item("Camiseta", "79.90", 2, "0.30"),
            item("Tenis", "249.90", 1, "1.20")
        ), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        CheckoutResposta out = sucesso(r);
        eq("409.70", out.subtotalProdutos());
        eq("0.00", out.descontoCupom());
        eq("15.60", out.frete());
        assertEquals(7, out.prazoEntregaDias());
        eq("6.15", out.seguro());
        eq("30.55", out.ajustePagamento());
        eq("462.00", out.totalFinal());
        assertEquals(6, out.parcelas());
        eq("77.00", out.valorParcela());
        eq("8.19", out.creditoProximaCompra());
        assertEquals(false, out.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        var r = req(List.of(
            item("Fone", "199.90", 2, "0.25")
        ), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        CheckoutResposta out = sucesso(r);
        eq("399.80", out.subtotalProdutos());
        eq("50.00", out.descontoCupom());
        eq("18.00", out.frete());
        assertEquals(0, out.prazoEntregaDias());
        eq("8.00", out.seguro());
        eq("3.49", out.ajustePagamento());
        eq("379.29", out.totalFinal());
        eq("379.29", out.valorParcela());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        var r = req(List.of(
            item("Meia", "19.90", 7, "0.10"),
            item("Camiseta", "79.90", 2, "0.30")
        ), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        CheckoutResposta out = sucesso(r);
        eq("299.10", out.subtotalProdutos());
        eq("39.80", out.descontoCupom());
        eq("0.00", out.frete());
        assertEquals(1, out.prazoEntregaDias());
        eq("2.99", out.seguro());
        eq("0.00", out.ajustePagamento());
        eq("262.29", out.totalFinal());
        eq("87.43", out.valorParcela());
        eq("5.98", out.creditoProximaCompra());
    }

    @Test
    void exemplo5_expressa_pix_ouro_sudeste() {
        var r = req(List.of(
            item("Camiseta", "79.90", 2, "0.30"),
            item("Tenis", "249.90", 1, "1.20")
        ), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        CheckoutResposta out = sucesso(r);
        eq("409.70", out.subtotalProdutos());
        eq("0.00", out.descontoCupom());
        eq("0.00", out.frete());
        assertEquals(2, out.prazoEntregaDias());
        eq("4.10", out.seguro());
        eq("-20.69", out.ajustePagamento());
        eq("393.11", out.totalFinal());
        eq("20.48", out.creditoProximaCompra());
        assertEquals(false, out.brinde());
    }

    @Test
    void pedido_vazio_resulta_em_pedido_invalido() {
        var r = req(new ArrayList<>(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals(CheckoutErro.PEDIDO_INVALIDO, erro(r));
    }

    @Test
    void motoboy_acima_de_5kg_resulta_em_modalidade_indisponivel() {
        var r = req(List.of(item("Caixa", "10.00", 1, "6")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals(CheckoutErro.MODALIDADE_INDISPONIVEL, erro(r));
    }

    @Test
    void menos50_abaixo_de_300_resulta_em_cupom_nao_aplicavel() {
        var r = req(List.of(item("Camiseta", "50.00", 1, "0.3")),
            "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals(CheckoutErro.CUPOM_NAO_APLICAVEL, erro(r));
    }

    @Test
    void boleto_acima_de_1000_resulta_em_forma_indisponivel() {
        var r = req(List.of(item("Notebook", "1200.00", 1, "2")),
            "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertEquals(CheckoutErro.FORMA_PAGAMENTO_INDISPONIVEL, erro(r));
    }

    @Test
    void cartao_com_13_parcelas_resulta_em_parcelamento_invalido() {
        var r = req(List.of(item("Camiseta", "79.90", 1, "0.3")),
            "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");
        assertEquals(CheckoutErro.PARCELAMENTO_INVALIDO, erro(r));
    }

    @Test
    void cupom_desconhecido_resulta_em_cupom_invalido() {
        var r = req(List.of(item("Camiseta", "79.90", 1, "0.3")),
            "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals(CheckoutErro.CUPOM_INVALIDO, erro(r));
    }
}
