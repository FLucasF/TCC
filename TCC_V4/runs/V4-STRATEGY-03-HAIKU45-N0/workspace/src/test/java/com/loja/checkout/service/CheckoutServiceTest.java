package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequisicao;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    public void setup() {
        service = new CheckoutService();
    }

    @Test
    public void exemplo1() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequisicao("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("BEMVINDO10");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        var resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("391.47"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo2() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequisicao("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("ECONOMICA");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(6);
        requisicao.setNivelClube("PRATA");
        requisicao.setRegiao("CENTRO_OESTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        var resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(BigDecimal.ZERO, resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resposta.getSeguro());
        assertEquals(new BigDecimal("30.55"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("77.00"), resposta.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo3() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORDESTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        var resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resposta.getSeguro());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("379.29"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo4() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("LEVE3PAGUE2");
        requisicao.setFormaPagamento("CARTAO");
        requisicao.setParcelas(3);
        requisicao.setNivelClube("PRATA");
        requisicao.setRegiao("SUL");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        var resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(BigDecimal.ZERO, resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resposta.getSeguro());
        assertEquals(BigDecimal.ZERO, resposta.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("87.43"), resposta.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemplo5() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequisicao("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("OURO");
        requisicao.setRegiao("SUDESTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaCheckout);

        var resposta = (RespostaCheckout) resultado;
        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(BigDecimal.ZERO, resposta.getDescontoCupom());
        assertEquals(BigDecimal.ZERO, resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("393.11"), resposta.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }
}
