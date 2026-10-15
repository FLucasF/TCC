package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.exception.CheckoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    public void setUp() {
        checkoutService = new CheckoutService();
    }

    @Test
    public void testExemplo1() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(Integer.valueOf(2), response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("44.25"), response.getImposto());
        assertEquals(new BigDecimal("-22.30"), response.getAjustePagamento());
        assertEquals(new BigDecimal("423.78"), response.getTotalFinal());
        assertEquals(Integer.valueOf(1), response.getParcelas());
        assertEquals(new BigDecimal("423.78"), response.getValorParcela());
    }

    @Test
    public void testExemplo2() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(0, response.getDescontoCupom().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(Integer.valueOf(7), response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("49.16"), response.getImposto());
        assertTrue(response.getAjustePagamento().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getTotalFinal().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(Integer.valueOf(6), response.getParcelas());
    }

    @Test
    public void testExemplo3() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(Integer.valueOf(0), response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("41.98"), response.getImposto());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("413.27"), response.getTotalFinal());
    }

    @Test
    public void testExemplo4() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Meia", 19.90, 7, 0.10),
            new ItemRequest("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(0, response.getFrete().compareTo(BigDecimal.ZERO));
        assertEquals(Integer.valueOf(1), response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("31.12"), response.getImposto());
        assertEquals(0, response.getAjustePagamento().compareTo(BigDecimal.ZERO));
        assertTrue(response.getTotalFinal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testExemplo5() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(0, response.getDescontoCupom().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getFrete().compareTo(BigDecimal.ZERO));
        assertEquals(Integer.valueOf(2), response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("49.16"), response.getImposto());
        assertEquals(new BigDecimal("-22.94"), response.getAjustePagamento());
        assertEquals(new BigDecimal("435.92"), response.getTotalFinal());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testCarrinhVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });
    }

    @Test
    public void testItemComPrecoZero() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 0.0, 1, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });
    }

    @Test
    public void testNivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("INVALIDO");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });
    }

    @Test
    public void testCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });
    }

    @Test
    public void testMotoboySobrepesoIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Produto Pesado", 100.0, 10, 1.0)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });
    }

    @Test
    public void testBoletoAcimaLimit() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Produto Caro", 1100.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });
    }

    @Test
    public void testParcelamentoPIXInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(3);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });
    }
}
