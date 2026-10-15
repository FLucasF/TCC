package com.exemplo.checkout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest item(String n, String p, int q, String peso) {
        return new ItemRequest(n, new BigDecimal(p), q, new BigDecimal(peso));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"),
                        item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        var r = service.calcular(req);
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
    void exemplo2_economica_sem_cupom_cartao6x_prata_centrooeste() {
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"),
                        item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        var r = service.calcular(req);
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
        var req = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        var r = service.calcular(req);
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
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        var req = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"),
                        item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        var r = service.calcular(req);
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
        var req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"),
                        item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        var r = service.calcular(req);
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

    @Test
    void erro_pedido_invalido_carrinho_vazio() {
        var req = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erro_nivel_invalido() {
        var req = new ResumoRequest(List.of(item("x", "10.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("NIVEL_CLUBE_INVALIDO", ex.getCodigo());
    }

    @Test
    void erro_regiao_invalida() {
        var req = new ResumoRequest(List.of(item("x", "10.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", null);
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("REGIAO_INVALIDA", ex.getCodigo());
    }

    @Test
    void erro_modalidade_indisponivel_motoboy_pesado() {
        var req = new ResumoRequest(List.of(item("x", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void erro_cupom_invalido() {
        var req = new ResumoRequest(List.of(item("x", "10.00", 1, "1.00")),
                "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    void erro_cupom_nao_aplicavel() {
        var req = new ResumoRequest(List.of(item("x", "100.00", 1, "1.00")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigo());
    }

    @Test
    void erro_parcelamento_pix() {
        var req = new ResumoRequest(List.of(item("x", "10.00", 1, "1.00")),
                "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void erro_boleto_acima_de_mil() {
        var req = new ResumoRequest(List.of(item("x", "600.00", 2, "1.00")),
                "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }
}
