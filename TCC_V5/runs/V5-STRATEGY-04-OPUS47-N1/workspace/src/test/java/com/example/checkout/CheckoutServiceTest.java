package com.example.checkout;

import com.example.checkout.model.Item;
import com.example.checkout.model.ResumoRequest;
import com.example.checkout.model.ResumoResponse;
import com.example.checkout.service.CheckoutException;
import com.example.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static Item item(String nome, String preco, int qtd, String peso) {
        return new Item(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal v(String s) {
        return new BigDecimal(s);
    }

    @Test
    void exemplo1() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
            "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"
        );
        ResumoResponse r = service.calcular(req);
        assertEquals(v("409.70"), r.subtotalProdutos());
        assertEquals(v("40.97"), r.descontoCupom());
        assertEquals(v("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(v("10.24"), r.seguro());
        assertEquals(v("-20.60"), r.ajustePagamento());
        assertEquals(v("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(v("391.47"), r.valorParcela());
        assertEquals(v("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo2() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
            "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );
        ResumoResponse r = service.calcular(req);
        assertEquals(v("409.70"), r.subtotalProdutos());
        assertEquals(v("0.00"), r.descontoCupom());
        assertEquals(v("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(v("6.15"), r.seguro());
        assertEquals(v("30.55"), r.ajustePagamento());
        assertEquals(v("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(v("77.00"), r.valorParcela());
        assertEquals(v("8.19"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo3() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Fone", "199.90", 2, "0.25")),
            "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"
        );
        ResumoResponse r = service.calcular(req);
        assertEquals(v("399.80"), r.subtotalProdutos());
        assertEquals(v("50.00"), r.descontoCupom());
        assertEquals(v("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(v("8.00"), r.seguro());
        assertEquals(v("3.49"), r.ajustePagamento());
        assertEquals(v("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(v("379.29"), r.valorParcela());
        assertEquals(v("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo4() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
            "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );
        ResumoResponse r = service.calcular(req);
        assertEquals(v("299.10"), r.subtotalProdutos());
        assertEquals(v("39.80"), r.descontoCupom());
        assertEquals(v("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(v("2.99"), r.seguro());
        assertEquals(v("0.00"), r.ajustePagamento());
        assertEquals(v("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(v("87.43"), r.valorParcela());
        assertEquals(v("5.98"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo5() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
            "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"
        );
        ResumoResponse r = service.calcular(req);
        assertEquals(v("409.70"), r.subtotalProdutos());
        assertEquals(v("0.00"), r.descontoCupom());
        assertEquals(v("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(v("4.10"), r.seguro());
        assertEquals(v("-20.69"), r.ajustePagamento());
        assertEquals(v("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(v("393.11"), r.valorParcela());
        assertEquals(v("20.48"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void carrinhoVazio() {
        ResumoRequest req = new ResumoRequest(
            List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void motoboyAcimaDe5Kg() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Haltere", "100.00", 1, "6.00")),
            "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("MODALIDADE_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void cupomMenos50AbaixoDe300() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30")),
            "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_NAO_APLICAVEL", e.getCodigo());
    }

    @Test
    void boletoAcimaDe1000() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("TV", "1200.00", 1, "5.00")),
            "ECONOMICA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void pixComMaisDeUmaParcela() {
        ResumoRequest req = new ResumoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30")),
            "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }
}
