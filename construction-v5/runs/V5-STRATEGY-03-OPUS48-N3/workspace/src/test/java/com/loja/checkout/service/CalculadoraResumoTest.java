package com.loja.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loja.checkout.domain.PedidoInvalidoException;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void exemplo1() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = calculadora.calcular(req);

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
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = calculadora.calcular(req);

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
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3() {
        var req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse r = calculadora.calcular(req);

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
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4() {
        var req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = calculadora.calcular(req);

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
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

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
        assertFalse(r.brinde());
    }

    @Test
    void freteGratisDescontaOValorDoFreteMostrado() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertEquals(v("33.10"), r.frete());
        assertEquals(r.frete(), r.descontoCupom());
    }

    @Test
    void freteGratisComOuroZeraFreteEDesconto() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertEquals(v("0.00"), r.frete());
        assertEquals(v("0.00"), r.descontoCupom());
    }

    @Test
    void cartaoAte3xNaoTemJuros() {
        var req = new CheckoutRequest(
                List.of(item("Tenis", "300.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertEquals(v("0.00"), r.ajustePagamento());
        assertEquals(r.totalFinal(), r.valorParcela().multiply(new BigDecimal("3")));
    }

    @Test
    void cartaoDe4xTemJurosPrice() {
        var req = new CheckoutRequest(
                List.of(item("Tenis", "300.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 4, "BRONZE", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertTrue(r.ajustePagamento().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(r.totalFinal(), r.valorParcela().multiply(new BigDecimal("4")));
    }

    @Test
    void cartaoEm12xEhPermitido() {
        var req = new CheckoutRequest(
                List.of(item("Tenis", "300.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE");

        ResumoResponse r = calculadora.calcular(req);

        assertEquals(12, r.parcelas());
        assertEquals(r.totalFinal(), r.valorParcela().multiply(new BigDecimal("12")));
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        var req = new CheckoutRequest(
                List.of(item("Casaco", "300.00", 2, "0.50")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        assertTrue(calculadora.calcular(req).brinde());
    }

    private String erro(CheckoutRequest req) {
        try {
            calculadora.calcular(req);
            throw new AssertionError("esperava recusa");
        } catch (PedidoInvalidoException e) {
            return e.getCodigo();
        }
    }

    @Test
    void carrinhoVazio() {
        assertEquals("PEDIDO_INVALIDO", erro(new CheckoutRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE")));
    }

    @Test
    void itemComQuantidadeZero() {
        assertEquals("PEDIDO_INVALIDO", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 0, "0.10")), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE")));
    }

    @Test
    void nivelClubeInvalido() {
        assertEquals("NIVEL_CLUBE_INVALIDO", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "NORTE")));
    }

    @Test
    void regiaoAusente() {
        assertEquals("REGIAO_INVALIDA", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1, "BRONZE", null)));
    }

    @Test
    void modalidadeInvalida() {
        assertEquals("MODALIDADE_INVALIDA", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")), "DRONE", null, "PIX", 1, "BRONZE", "NORTE")));
    }

    @Test
    void motoboyAcimaDe5kg() {
        assertEquals("MODALIDADE_INDISPONIVEL", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 6, "1.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE")));
    }

    @Test
    void cupomInexistente() {
        assertEquals("CUPOM_INVALIDO", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")), "EXPRESSA", "NATAL", "PIX", 1, "BRONZE", "NORTE")));
    }

    @Test
    void menos50AbaixoDoMinimo() {
        assertEquals("CUPOM_NAO_APLICAVEL", erro(new CheckoutRequest(
                List.of(item("X", "100.00", 1, "0.10")), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE")));
    }

    @Test
    void formaPagamentoInvalida() {
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "NORTE")));
    }

    @Test
    void pixParcelado() {
        assertEquals("PARCELAMENTO_INVALIDO", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")), "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE")));
    }

    @Test
    void cartaoAcimaDe12x() {
        assertEquals("PARCELAMENTO_INVALIDO", erro(new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "NORTE")));
    }

    @Test
    void boletoAcimaDe1000() {
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro(new CheckoutRequest(
                List.of(item("X", "2000.00", 1, "0.10")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")));
    }
}
