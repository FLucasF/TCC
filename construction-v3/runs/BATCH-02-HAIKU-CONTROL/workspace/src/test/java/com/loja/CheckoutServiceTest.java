package com.loja;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.exception.CheckoutException;
import com.loja.service.CheckoutService;
import com.loja.service.ValidadorCheckout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        ValidadorCheckout validador = new ValidadorCheckout();
        checkoutService = new CheckoutService(validador);
    }

    @Test
    void exemplo1() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calcular(request);

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
    void exemplo2() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("75.90"), response.getValorParcela());
    }

    @Test
    void exemplo3() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calcular(request);

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
    void exemplo4() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    void erroCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroItemComPrecoNegativo() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("-10.00"), 1, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroModalidadeInvalida() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroMotoboyCom6Kg() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Produto", new BigDecimal("100.00"), 1, new BigDecimal("6.00")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroCupomInvalido() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroCupomMenos50ComSubtotalBaixo() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroFormaPagamentoInvalida() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroParcelamentoPix() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(3);

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    void erroBoletoAcima1000Reais() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(new ItemRequest("Produto", new BigDecimal("1100.00"), 1, new BigDecimal("1.00")));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");

        assertThrows(CheckoutException.class, () -> checkoutService.calcular(request));
    }
}
