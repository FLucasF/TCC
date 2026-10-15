package com.loja;

import com.loja.dominio.CalculadoraResumo;
import com.loja.dominio.ErroCheckout;
import com.loja.dominio.Resumo;
import com.loja.web.ItemDTO;
import com.loja.web.PedidoDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraResumoTest {

    private final CalculadoraResumo calc = new CalculadoraResumo();

    private static ItemDTO item(String n, String preco, int qtd, String peso) {
        return new ItemDTO(n, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        var pedido = new PedidoDTO(
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
            "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"
        );
        Resumo r = calc.calcular(pedido);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("40.97"), r.descontoCupom());
        assertEquals(bd("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(bd("10.24"), r.seguro());
        assertEquals(bd("-20.60"), r.ajustePagamento());
        assertEquals(bd("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("391.47"), r.valorParcela());
        assertEquals(bd("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6_prata_centrooeste() {
        var pedido = new PedidoDTO(
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
            "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );
        Resumo r = calc.calcular(pedido);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.descontoCupom());
        assertEquals(bd("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(bd("6.15"), r.seguro());
        assertEquals(bd("30.55"), r.ajustePagamento());
        assertEquals(bd("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(bd("77.00"), r.valorParcela());
        assertEquals(bd("8.19"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        var pedido = new PedidoDTO(
            List.of(item("Fone", "199.90", 2, "0.25")),
            "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );
        Resumo r = calc.calcular(pedido);
        assertEquals(bd("399.80"), r.subtotalProdutos());
        assertEquals(bd("50.00"), r.descontoCupom());
        assertEquals(bd("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(bd("8.00"), r.seguro());
        assertEquals(bd("3.49"), r.ajustePagamento());
        assertEquals(bd("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("379.29"), r.valorParcela());
        assertEquals(bd("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3_prata_sul() {
        var pedido = new PedidoDTO(
            List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
            "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );
        Resumo r = calc.calcular(pedido);
        assertEquals(bd("299.10"), r.subtotalProdutos());
        assertEquals(bd("39.80"), r.descontoCupom());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(bd("2.99"), r.seguro());
        assertEquals(bd("0.00"), r.ajustePagamento());
        assertEquals(bd("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(bd("87.43"), r.valorParcela());
        assertEquals(bd("5.98"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        var pedido = new PedidoDTO(
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
            "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"
        );
        Resumo r = calc.calcular(pedido);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.descontoCupom());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(bd("4.10"), r.seguro());
        assertEquals(bd("-20.69"), r.ajustePagamento());
        assertEquals(bd("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("393.11"), r.valorParcela());
        assertEquals(bd("20.48"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    private PedidoDTO base() {
        return new PedidoDTO(
            List.of(item("x", "100.00", 1, "0.50")),
            "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"
        );
    }

    private String erro(PedidoDTO p) {
        try { calc.calcular(p); return null; } catch (ErroCheckout e) { return e.codigo(); }
    }

    @Test void erro_pedido_invalido_carrinho_vazio() {
        var p = new PedidoDTO(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", erro(p));
    }

    @Test void erro_pedido_invalido_peso_zero() {
        var p = new PedidoDTO(List.of(item("x","1.00",1,"0")), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", erro(p));
    }

    @Test void erro_nivel_clube_invalido() {
        var b = base();
        var p = new PedidoDTO(b.itens(), b.modalidadeEntrega(), b.cupom(), b.formaPagamento(), b.parcelas(), "DIAMANTE", b.regiao());
        assertEquals("NIVEL_CLUBE_INVALIDO", erro(p));
    }

    @Test void erro_regiao_invalida() {
        var b = base();
        var p = new PedidoDTO(b.itens(), b.modalidadeEntrega(), b.cupom(), b.formaPagamento(), b.parcelas(), b.nivelClube(), null);
        assertEquals("REGIAO_INVALIDA", erro(p));
    }

    @Test void erro_modalidade_invalida() {
        var b = base();
        var p = new PedidoDTO(b.itens(), "DRONE", b.cupom(), b.formaPagamento(), b.parcelas(), b.nivelClube(), b.regiao());
        assertEquals("MODALIDADE_INVALIDA", erro(p));
    }

    @Test void erro_modalidade_indisponivel_motoboy_acima_5kg() {
        var p = new PedidoDTO(List.of(item("x","1.00",10,"1.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INDISPONIVEL", erro(p));
    }

    @Test void erro_cupom_invalido() {
        var b = base();
        var p = new PedidoDTO(b.itens(), b.modalidadeEntrega(), "NAOEXISTE", b.formaPagamento(), b.parcelas(), b.nivelClube(), b.regiao());
        assertEquals("CUPOM_INVALIDO", erro(p));
    }

    @Test void erro_cupom_nao_aplicavel_menos50_abaixo_300() {
        var p = new PedidoDTO(List.of(item("x","50.00",1,"0.5")), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_NAO_APLICAVEL", erro(p));
    }

    @Test void erro_forma_pagamento_invalida() {
        var b = base();
        var p = new PedidoDTO(b.itens(), b.modalidadeEntrega(), b.cupom(), "DOGECOIN", b.parcelas(), b.nivelClube(), b.regiao());
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro(p));
    }

    @Test void erro_parcelamento_invalido_pix_2x() {
        var b = base();
        var p = new PedidoDTO(b.itens(), b.modalidadeEntrega(), b.cupom(), "PIX", 2, b.nivelClube(), b.regiao());
        assertEquals("PARCELAMENTO_INVALIDO", erro(p));
    }

    @Test void erro_parcelamento_invalido_cartao_13x() {
        var b = base();
        var p = new PedidoDTO(b.itens(), b.modalidadeEntrega(), b.cupom(), "CARTAO", 13, b.nivelClube(), b.regiao());
        assertEquals("PARCELAMENTO_INVALIDO", erro(p));
    }

    @Test void erro_forma_pagamento_indisponivel_boleto_acima_1000() {
        var p = new PedidoDTO(List.of(item("x","600.00",2,"0.5")), "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro(p));
    }
}
