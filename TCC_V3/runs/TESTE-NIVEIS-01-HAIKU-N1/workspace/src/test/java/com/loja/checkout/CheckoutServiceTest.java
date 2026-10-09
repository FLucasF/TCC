package com.loja.checkout;

import com.loja.checkout.model.*;
import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        Object resultado = service.processar(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.getSeguro());
        assertEquals(new BigDecimal("-20.60"), response.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("391.47"), response.getValorParcela());
        assertEquals(0, response.getCreditoProximaCompra().compareTo(BigDecimal.ZERO));
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo2() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setCupom(null);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(6);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.CENTRO_OESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(0, response.getDescontoCupom().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.55"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.00"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo3() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo4() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Meia", 19.90, 7, 0.10),
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(3);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.SUL);

        Object resultado = service.processar(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(0, response.getFrete().compareTo(BigDecimal.ZERO));
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.getSeguro());
        assertEquals(0, response.getAjustePagamento().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("262.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("87.43"), response.getValorParcela());
        assertEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
        assertEquals(0, response.getAjustePagamento().compareTo(BigDecimal.ZERO));
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo5() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom(null);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.OURO);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(CheckoutResponse.class, resultado);

        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(0, response.getDescontoCupom().compareTo(BigDecimal.ZERO));
        assertEquals(0, response.getFrete().compareTo(BigDecimal.ZERO));
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.getSeguro());
        assertEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("393.11"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }
}
