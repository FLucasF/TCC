package com.loja;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.exception.CheckoutException;
import com.loja.service.CheckoutService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1() {
        var request = new CheckoutRequest(
                List.of(
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"
        );

        CheckoutResponse r = service.calcular(request);

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
    void exemplo2() {
        var request = new CheckoutRequest(
                List.of(
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );

        CheckoutResponse r = service.calcular(request);

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
    void exemplo3() {
        var request = new CheckoutRequest(
                List.of(
                        new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
                ),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );

        CheckoutResponse r = service.calcular(request);

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
    void exemplo4() {
        var request = new CheckoutRequest(
                List.of(
                        new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
                ),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );

        CheckoutResponse r = service.calcular(request);

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
    void exemplo5() {
        var request = new CheckoutRequest(
                List.of(
                        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"
        );

        CheckoutResponse r = service.calcular(request);

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
    void carrinhoVazio() {
        var request = new CheckoutRequest(Collections.emptyList(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void itemComPrecoZero() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", BigDecimal.ZERO, 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void nivelClubeInvalido() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", e.getCodigo());
    }

    @Test
    void regiaoInvalida() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", null, "BRONZE", "LESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("REGIAO_INVALIDA", e.getCodigo());
    }

    @Test
    void modalidadeInvalida() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"))),
                "DRONE", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INVALIDA", e.getCodigo());
    }

    @Test
    void motoboyAcimaDe5kg() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("Pesado", new BigDecimal("100"), 1, new BigDecimal("6.0"))),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void cupomInvalido() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", "INVALIDO", "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_INVALIDO", e.getCodigo());
    }

    @Test
    void menos50AbaixoDe300() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("100"), 2, new BigDecimal("0.5"))),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_NAO_APLICAVEL", e.getCodigo());
    }

    @Test
    void formaPagamentoInvalida() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", e.getCodigo());
    }

    @Test
    void parcelamentoInvalidoPix() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void parcelamentoInvalidoCartao13x() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void boletoAcimaDe1000() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("Caro", new BigDecimal("600"), 2, new BigDecimal("1.0"))),
                "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUDESTE"
        );
        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void ouroBrindeAcima500() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("Premium", new BigDecimal("510"), 1, new BigDecimal("1.0"))),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE"
        );
        CheckoutResponse r = service.calcular(request);
        assertTrue(r.brinde());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(bd("25.50"), r.creditoProximaCompra());
    }

    @Test
    void fretegratisSemClube() {
        var request = new CheckoutRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))),
                "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutResponse r = service.calcular(request);
        BigDecimal freteEsperado = bd("13.20");
        assertEquals(freteEsperado, r.frete());
        assertEquals(freteEsperado, r.descontoCupom());
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
