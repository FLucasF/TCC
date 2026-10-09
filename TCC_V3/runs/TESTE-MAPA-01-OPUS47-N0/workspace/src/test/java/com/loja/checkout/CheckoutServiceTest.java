package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static CheckoutRequest.Item item(String nome, String preco, int qtd, String peso) {
        return new CheckoutRequest.Item(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
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
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
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
        assertEquals(6, r.parcelas());
        assertEquals(bd("77.00"), r.valorParcela());
        assertEquals(bd("8.19"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
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
        assertEquals(1, r.parcelas());
        assertEquals(bd("379.29"), r.valorParcela());
        assertEquals(bd("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
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
        assertEquals(3, r.parcelas());
        assertEquals(bd("87.43"), r.valorParcela());
        assertEquals(bd("5.98"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
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
        assertEquals(false, r.brinde());
    }

    @Test
    void carrinhoVazio_pedidoInvalido() {
        var req = new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void motoboyAcimaDe5kg_indisponivel() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "6")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void cupomInvalido() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.1")),
                "ECONOMICA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    void menos50_abaixoLimite_naoAplicavel() {
        var req = new CheckoutRequest(
                List.of(item("X", "100.00", 1, "0.1")),
                "ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigo());
    }

    @Test
    void boletoAcimaDe1000_indisponivel() {
        var req = new CheckoutRequest(
                List.of(item("X", "600.00", 2, "0.1")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void pixComMaisDeUmaParcela_invalido() {
        var req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.1")),
                "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }
}
