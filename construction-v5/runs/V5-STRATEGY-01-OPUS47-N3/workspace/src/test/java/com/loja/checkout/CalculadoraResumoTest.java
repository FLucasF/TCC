package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutRequest.ItemRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraResumoTest {

    private final CalculadoraResumo calc = new CalculadoraResumo();

    private static BigDecimal d(String s) { return new BigDecimal(s); }

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, d(preco), qtd, d(peso));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");
        var r = calc.calcular(req);
        assertEquals(d("409.70"), r.subtotalProdutos());
        assertEquals(d("40.97"), r.descontoCupom());
        assertEquals(d("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(d("10.24"), r.seguro());
        assertEquals(d("-20.60"), r.ajustePagamento());
        assertEquals(d("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(d("391.47"), r.valorParcela());
        assertEquals(d("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_co() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        var r = calc.calcular(req);
        assertEquals(d("409.70"), r.subtotalProdutos());
        assertEquals(d("0.00"), r.descontoCupom());
        assertEquals(d("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(d("6.15"), r.seguro());
        assertEquals(d("30.55"), r.ajustePagamento());
        assertEquals(d("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(d("77.00"), r.valorParcela());
        assertEquals(d("8.19"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        var req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");
        var r = calc.calcular(req);
        assertEquals(d("399.80"), r.subtotalProdutos());
        assertEquals(d("50.00"), r.descontoCupom());
        assertEquals(d("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(d("8.00"), r.seguro());
        assertEquals(d("3.49"), r.ajustePagamento());
        assertEquals(d("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(d("379.29"), r.valorParcela());
        assertEquals(d("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        var req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        var r = calc.calcular(req);
        assertEquals(d("299.10"), r.subtotalProdutos());
        assertEquals(d("39.80"), r.descontoCupom());
        assertEquals(d("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(d("2.99"), r.seguro());
        assertEquals(d("0.00"), r.ajustePagamento());
        assertEquals(d("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(d("87.43"), r.valorParcela());
        assertEquals(d("5.98"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");
        var r = calc.calcular(req);
        assertEquals(d("409.70"), r.subtotalProdutos());
        assertEquals(d("0.00"), r.descontoCupom());
        assertEquals(d("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(d("4.10"), r.seguro());
        assertEquals(d("-20.69"), r.ajustePagamento());
        assertEquals(d("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(d("393.11"), r.valorParcela());
        assertEquals(d("20.48"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void cupom_fretegratis_desconta_valor_do_frete() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");
        var r = calc.calcular(req);
        assertEquals(d("33.10"), r.frete());
        assertEquals(d("33.10"), r.descontoCupom());
    }

    @Test
    void cupom_fretegratis_com_ouro_desconto_zero() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE");
        var r = calc.calcular(req);
        assertEquals(d("0.00"), r.frete());
        assertEquals(d("0.00"), r.descontoCupom());
    }

    @Test
    void brinde_ouro_acima_500() {
        var req = new CheckoutRequest(
                List.of(item("Casaco", "600.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");
        var r = calc.calcular(req);
        assertTrue(r.brinde());
    }

    @Test
    void erro_pedido_vazio() {
        var req = new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_item_preco_zero() {
        var req = new CheckoutRequest(
                List.of(item("X", "0.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_nivel_invalido() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE");
        assertEquals("NIVEL_CLUBE_INVALIDO", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_regiao_invalida() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", null, "BRONZE", "LESTE");
        assertEquals("REGIAO_INVALIDA", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_modalidade_invalida() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "DRONE", null, "PIX", null, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INVALIDA", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_motoboy_acima_5kg() {
        var req = new CheckoutRequest(
                List.of(item("Pesado", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INDISPONIVEL", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_cupom_invalido() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", "INEXISTENTE", "PIX", null, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_INVALIDO", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_cupom_nao_aplicavel() {
        var req = new CheckoutRequest(
                List.of(item("X", "100.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_NAO_APLICAVEL", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_forma_pagamento_invalida() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INVALIDA", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_parcelamento_invalido_pix() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_parcelamento_invalido_cartao_13() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }

    @Test
    void erro_boleto_acima_1000() {
        var req = new CheckoutRequest(
                List.of(item("X", "2000.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", assertThrows(ErroPedido.class, () -> calc.calcular(req)).codigo());
    }
}
