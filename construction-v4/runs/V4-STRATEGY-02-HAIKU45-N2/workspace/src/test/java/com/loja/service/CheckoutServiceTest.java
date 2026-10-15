package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemCarrinho;
import com.loja.exception.CheckoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        var response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.descontoCupom());
        assertEquals(new BigDecimal("33.10"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.seguro());
        assertEquals(new BigDecimal("-20.60"), response.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("391.47"), response.valorParcela());
        assertEquals(new BigDecimal("0.00"), response.creditoProximaCompra());
        assertEquals(false, response.brinde());
    }

    @Test
    void exemplo2() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
        var request = new CheckoutRequest(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        var response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.descontoCupom());
        assertEquals(new BigDecimal("15.60"), response.frete());
        assertEquals(7, response.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.seguro());
        assertEquals(new BigDecimal("30.55"), response.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), response.totalFinal());
        assertEquals(6, response.parcelas());
        assertEquals(new BigDecimal("77.00"), response.valorParcela());
        assertEquals(new BigDecimal("8.19"), response.creditoProximaCompra());
        assertEquals(false, response.brinde());
    }

    @Test
    void exemplo3() {
        var itens = List.of(
                new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );
        var request = new CheckoutRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        var response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.descontoCupom());
        assertEquals(new BigDecimal("18.00"), response.frete());
        assertEquals(0, response.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.seguro());
        assertEquals(new BigDecimal("3.49"), response.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("379.29"), response.valorParcela());
        assertEquals(new BigDecimal("0.00"), response.creditoProximaCompra());
        assertEquals(false, response.brinde());
    }

    @Test
    void exemplo4() {
        var itens = List.of(
                new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        var response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(1, response.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.seguro());
        assertEquals(new BigDecimal("0.00"), response.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), response.totalFinal());
        assertEquals(3, response.parcelas());
        assertEquals(new BigDecimal("87.43"), response.valorParcela());
        assertEquals(new BigDecimal("5.98"), response.creditoProximaCompra());
        assertEquals(false, response.brinde());
    }

    @Test
    void exemplo5() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        var response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.descontoCupom());
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(2, response.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.seguro());
        assertEquals(new BigDecimal("-20.69"), response.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), response.totalFinal());
        assertEquals(1, response.parcelas());
        assertEquals(new BigDecimal("393.11"), response.valorParcela());
        assertEquals(new BigDecimal("20.48"), response.creditoProximaCompra());
        assertEquals(false, response.brinde());
    }

    @Test
    void pedidoInvalido_carrinhoVazio() {
        var itens = List.<ItemCarrinho>of();
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void pedidoInvalido_precoZero() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("0.00"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void pedidoInvalido_quantidadeZero() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void nivelClubeInvalido() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "PIX", 1, "INVALIDO", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void regiaoInvalida() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "INVALIDA");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("REGIAO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void modalidadeInvalida() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "INVALIDA", null, "PIX", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void modalidadeIndisponivel_motoboyAcima5kg() {
        var itens = List.of(
                new ItemCarrinho("Produto", new BigDecimal("79.90"), 2, new BigDecimal("3.00"))
        );
        var request = new CheckoutRequest(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void cupomInvalido() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", "INVALIDO", "PIX", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void cupomNaoAplicavel_menos50() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    void formaPagamentoInvalida() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "INVALIDA", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void parcelamentoInvalido_pixComParcelas() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void parcelamentoInvalido_cartaoAcima12() {
        var itens = List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcima1000() {
        var itens = List.of(
                new ItemCarrinho("Produto", new BigDecimal("1000.01"), 1, new BigDecimal("0.30"))
        );
        var request = new CheckoutRequest(itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "NORTE");

        var exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }
}
