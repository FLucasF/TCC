package com.loja.checkout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static CheckoutRequest.Item item(String n, String p, int q, String kg) {
        return new CheckoutRequest.Item(n, new BigDecimal(p), q, new BigDecimal(kg));
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void exemplo1() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
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
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo2() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        var r = service.calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.descontoCupom());
        assertEquals(bd("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(bd("6.15"), r.seguro());
        assertEquals(bd("30.55"), r.ajustePagamento());
        assertEquals(bd("462.00"), r.totalFinal());
        assertEquals(bd("77.00"), r.valorParcela());
        assertEquals(bd("8.19"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo3() {
        var req = new CheckoutRequest(
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
    }

    @Test
    void exemplo4() {
        var req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        var r = service.calcular(req);
        assertEquals(bd("299.10"), r.subtotalProdutos());
        assertEquals(bd("39.80"), r.descontoCupom());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(bd("2.99"), r.seguro());
        assertEquals(bd("0.00"), r.ajustePagamento());
        assertEquals(bd("262.29"), r.totalFinal());
        assertEquals(bd("87.43"), r.valorParcela());
        assertEquals(bd("5.98"), r.creditoProximaCompra());
    }

    @Test
    void exemplo5() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        var r = service.calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(bd("4.10"), r.seguro());
        assertEquals(bd("-20.69"), r.ajustePagamento());
        assertEquals(bd("393.11"), r.totalFinal());
        assertEquals(bd("20.48"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void carrinhoVazio() {
        var req = new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PEDIDO_INVALIDO", ex.codigo());
    }

    @Test
    void motoboyAcimaDe5Kg() {
        var req = new CheckoutRequest(
                List.of(item("Pesado", "100.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.codigo());
    }

    @Test
    void boletoAcimaDe1000() {
        var req = new CheckoutRequest(
                List.of(item("Caro", "1500.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.codigo());
    }

    @Test
    void cupomMenos50AbaixoDe300() {
        var req = new CheckoutRequest(
                List.of(item("Barato", "100.00", 1, "0.10")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUL");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_NAO_APLICAVEL", ex.codigo());
    }

    @Test
    void cupomInvalido() {
        var req = new CheckoutRequest(
                List.of(item("X", "100.00", 1, "0.10")),
                "RETIRADA_LOJA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUL");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_INVALIDO", ex.codigo());
    }

    @Test
    void parcelamentoInvalidoPix() {
        var req = new CheckoutRequest(
                List.of(item("X", "100.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 3, "BRONZE", "SUL");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PARCELAMENTO_INVALIDO", ex.codigo());
    }
}
