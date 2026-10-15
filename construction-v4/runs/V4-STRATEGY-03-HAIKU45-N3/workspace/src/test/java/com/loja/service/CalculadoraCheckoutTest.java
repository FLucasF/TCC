package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraCheckoutTest {
    private final CalculadoraCheckout calculadora = new CalculadoraCheckout();

    @Test
    void exemplo1() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.getSeguro());
        assertEquals(new BigDecimal("-20.60"), response.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("391.47"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertEquals(false, response.isBrinde());
    }

    @Test
    void exemplo2() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.55"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.00"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertEquals(false, response.isBrinde());
    }

    @Test
    void exemplo3() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.getSeguro());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("379.29"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertEquals(false, response.isBrinde());
    }

    @Test
    void exemplo4() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.getSeguro());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("87.43"), response.getValorParcela());
        assertEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
        assertEquals(false, response.isBrinde());
    }

    @Test
    void exemplo5() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.getSeguro());
        assertEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("393.11"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertEquals(false, response.isBrinde());
    }

    @Test
    void testaFretegratisComCupom() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
    }

    @Test
    void testaOuroComBrinde() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Camiseta", new BigDecimal("300.00"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(true, response.isBrinde());
    }

    @Test
    void testaCupomMenos50Aplicavel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Item", new BigDecimal("150.00"), 2, new BigDecimal("0.10"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
    }

    @Test
    void testaBoletoBaixoValor() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("0.10"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = calculadora.calcular(request);

        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
    }
}
