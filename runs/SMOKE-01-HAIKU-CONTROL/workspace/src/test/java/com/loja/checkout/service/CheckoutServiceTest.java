package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    public void setUp() {
        service = new CheckoutService();
    }

    @Test
    public void testExemplo1() {
        ItemPedidoRequest item1 = new ItemPedidoRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
        ItemPedidoRequest item2 = new ItemPedidoRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item1, item2),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), response.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("381.74"), response.getValorParcela());
    }

    @Test
    public void testExemplo2() {
        ItemPedidoRequest item1 = new ItemPedidoRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
        ItemPedidoRequest item2 = new ItemPedidoRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item1, item2),
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(0, response.getDescontoCupom().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("75.90"), response.getValorParcela());
    }

    @Test
    public void testExemplo3() {
        ItemPedidoRequest item = new ItemPedidoRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("371.29"), response.getValorParcela());
    }

    @Test
    public void testExemplo4() {
        ItemPedidoRequest item1 = new ItemPedidoRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ItemPedidoRequest item2 = new ItemPedidoRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item1, item2),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-0.01"), response.getAjustePagamento());
        assertEquals(new BigDecimal("259.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    public void testCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest(
            new ArrayList<>(),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testItemComPrecoZero() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("0"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testItemComQuantidadeZero() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 0, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testItemComPesoZero() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testModalidadeInvalida() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "INVALIDA",
            null,
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testMotoboySobreoPesoLimite() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("5.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testCupomInvalido() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            "INVALIDO",
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testCupomMenos50AbaixoDeMinimo() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testFormaPagamentoInvalida() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "INVALIDA",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testParcelaPIXMaiorQueUm() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "PIX",
            2
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testParcelaBoletoMaiorQueUm() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "BOLETO",
            2
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testParcelaCartaoInvalida() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "CARTAO",
            13
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testBoletoAcimaDeLimite() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("1001.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "BOLETO",
            1
        );

        assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
    }

    @Test
    public void testPixComDesconto() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "RETIRADA_LOJA",
            null,
            "PIX",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        BigDecimal desconto5Porcento = new BigDecimal("5.00");
        assertEquals(desconto5Porcento.negate(), response.getAjustePagamento());
    }

    @Test
    public void testBoletoComTarifa() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
    }

    @Test
    public void testCupomFreteGratis() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("2.00"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            "FRETEGRATIS",
            "PIX",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(response.getFrete(), response.getDescontoCupom());
    }

    @Test
    public void testCupomLeve3Pague2() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("10.00"), 6, new BigDecimal("0.10"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "PIX",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);

        BigDecimal descontoEsperado = new BigDecimal("20.00");
        assertEquals(descontoEsperado, response.getDescontoCupom());
    }

    @Test
    public void testMotoboySoboPesoLimiteDe5kg() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("5.00"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);
        assertEquals(0, response.getPrazoEntregaDias());
    }

    @Test
    public void testParcelasDefault() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "CARTAO",
            null
        );

        CheckoutResponse response = service.calcularResumo(request);
        assertEquals(1, response.getParcelas());
    }

    @Test
    public void testCupomNulo() {
        ItemPedidoRequest item = new ItemPedidoRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"));
        CheckoutRequest request = new CheckoutRequest(
            Arrays.asList(item),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        CheckoutResponse response = service.calcularResumo(request);
        assertEquals(0, response.getDescontoCupom().compareTo(BigDecimal.ZERO));
    }
}
