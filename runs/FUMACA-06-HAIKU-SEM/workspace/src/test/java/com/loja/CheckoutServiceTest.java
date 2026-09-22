package com.loja;

import com.loja.dto.Item;
import com.loja.dto.ResumoCheckoutRequest;
import com.loja.dto.ResumoCheckoutResponse;
import com.loja.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {
    private CheckoutService checkoutService;

    @BeforeEach
    public void setUp() {
        checkoutService = new CheckoutService();
    }

    @Test
    public void exemplo1_camiseta_tenis_expressa_bemvindo10_pix() throws CheckoutService.CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        ResumoCheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(40.97, response.getDescontoCupom());
        assertEquals(33.10, response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(-20.09, response.getAjustePagamento());
        assertEquals(381.74, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(381.74, response.getValorParcela());
    }

    @Test
    public void exemplo2_camiseta_tenis_economica_cartao_6x() throws CheckoutService.CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        ResumoCheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(0.0, response.getDescontoCupom());
        assertEquals(15.60, response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(30.10, response.getAjustePagamento());
        assertEquals(455.40, response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(75.90, response.getValorParcela());
    }

    @Test
    public void exemplo3_fone_motoboy_menos50_boleto() throws CheckoutService.CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        ResumoCheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(399.80, response.getSubtotalProdutos());
        assertEquals(50.00, response.getDescontoCupom());
        assertEquals(18.00, response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(3.49, response.getAjustePagamento());
        assertEquals(371.29, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(371.29, response.getValorParcela());
    }

    @Test
    public void exemplo4_meia_camiseta_retirada_loja_leve3pague2_cartao_3x() throws CheckoutService.CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Meia", 19.90, 7, 0.10),
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        ResumoCheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(299.10, response.getSubtotalProdutos());
        assertEquals(39.80, response.getDescontoCupom());
        assertEquals(0.0, response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(0.0, response.getAjustePagamento());
        assertEquals(259.30, response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(86.43, response.getValorParcela());
    }

    @Test
    public void erro_pedido_invalido_item_sem_preco() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", null, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "PEDIDO_INVALIDO");
    }

    @Test
    public void erro_modalidade_invalida() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "MODALIDADE_INVALIDA");
    }

    @Test
    public void erro_cupom_invalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "CUPOM_INVALIDO");
    }

    @Test
    public void erro_forma_pagamento_invalida() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDO");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    public void erro_motoboy_acima_de_5kg() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", 100.0, 10, 0.6)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    public void erro_cupom_menos50_abaixo_de_300() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    public void erro_boleto_acima_de_1000() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", 500.0, 3, 1.0)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    public void erro_pix_parcelado() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(3);

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        }, "PARCELAMENTO_INVALIDO");
    }
}
