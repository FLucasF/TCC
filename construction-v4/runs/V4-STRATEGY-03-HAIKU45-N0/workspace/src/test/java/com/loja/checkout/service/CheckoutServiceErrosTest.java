package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequisicao;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaErro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckoutServiceErrosTest {

    private CheckoutService service;

    @BeforeEach
    public void setup() {
        service = new CheckoutService();
    }

    @Test
    public void pedidoInvalidoCarrinhoVazio() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Collections.emptyList());
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void pedidoInvalidoPrecoZero() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", BigDecimal.ZERO, 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void pedidoInvalidoQuantidadeZero() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PEDIDO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void nivelClubeInvalido() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("INVALIDO");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void regioInvalida() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("INVALIDA");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("REGIAO_INVALIDA", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void modalidadeInvalida() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("INVALIDA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("MODALIDADE_INVALIDA", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void modalidadeIndisponuelMotoboySobrepeso() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.00"))
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("MODALIDADE_INDISPONIVEL", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void cupomInvalido() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("CUPOMINVALIDO");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("CUPOM_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void cupomNaoAplicavel() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("100.00"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("PIX");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("CUPOM_NAO_APLICAVEL", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void formaPagamentoInvalida() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("INVALIDA");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void parcelamentoInvalidoPixAcima1() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(2);
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PARCELAMENTO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void parcelamentoInvalidoBoletoAcima1() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setParcelas(2);
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("PARCELAMENTO_INVALIDO", ((RespostaErro) resultado).getErro());
    }

    @Test
    public void formaPagamentoIndisponuelBoletoAcima1000() {
        var requisicao = new RequisicaoCheckout();
        requisicao.setItens(Arrays.asList(
            new ItemRequisicao("Produto caro", new BigDecimal("500.00"), 3, new BigDecimal("1.00"))
        ));
        requisicao.setModalidadeEntrega("EXPRESSA");
        requisicao.setFormaPagamento("BOLETO");
        requisicao.setNivelClube("BRONZE");
        requisicao.setRegiao("NORTE");

        var resultado = service.calcularResumo(requisicao);
        assertTrue(resultado instanceof RespostaErro);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((RespostaErro) resultado).getErro());
    }
}
