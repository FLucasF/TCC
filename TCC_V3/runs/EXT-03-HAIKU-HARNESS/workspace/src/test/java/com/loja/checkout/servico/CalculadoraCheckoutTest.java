package com.loja.checkout.servico;

import com.loja.checkout.dto.Item;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import com.loja.checkout.exception.ErroCheckout;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraCheckoutTest {

    private final CalculadoraCheckout calculadora = new CalculadoraCheckout();

    @Test
    void exemplo1_CamisetaTenisMotoExpressPix() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("BEMVINDO10");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), resposta.getImposto());
        assertEquals(new BigDecimal("-20.09"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("381.74"), resposta.getValorParcela());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(6);
        req.setNivelClube("BRONZE");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), resposta.getImposto());
        assertEquals(new BigDecimal("30.11"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("455.41"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("75.90"), resposta.getValorParcela());
    }

    @Test
    void exemplo3_FoneMotoboy() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setCupom("MENOS50");
        req.setFormaPagamento("BOLETO");
        req.setNivelClube("BRONZE");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), resposta.getImposto());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("371.29"), resposta.getValorParcela());
    }

    @Test
    void exemplo4_MeiaComLeve3Pague2() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("LEVE3PAGUE2");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(3);
        req.setNivelClube("BRONZE");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertEquals(new BigDecimal("259.30"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), resposta.getImposto());
        assertEquals(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("86.43"), resposta.getValorParcela());
    }

    @Test
    void exemplo5_OuroSemCupon() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("OURO");
        req.setRegiao("SUDESTE");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("49.16"), resposta.getImposto());
        assertEquals(new BigDecimal("-22.94"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("435.92"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("435.92"), resposta.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    void validacao_carrinhoVazio() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.emptyList());
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void validacao_itemComPrecoZero() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", BigDecimal.ZERO, 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void validacao_nivelClubeInvalido() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("INVALIDO");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("NIVEL_CLUBE_INVALIDO", e.getCodigo());
    }

    @Test
    void validacao_regiaoInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("INVALIDA");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("REGIAO_INVALIDA", e.getCodigo());
    }

    @Test
    void validacao_modalidadeInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("INVALIDA");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("MODALIDADE_INVALIDA", e.getCodigo());
    }

    @Test
    void validacao_modalidadeIndisponivel_motoboySobreLimit() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("10"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("MOTOBOY");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("MODALIDADE_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void validacao_cupomInvalido() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("INVALIDO");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("CUPOM_INVALIDO", e.getCodigo());
    }

    @Test
    void validacao_cupomNaoAplicavel_menos50() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("200"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("MENOS50");
        req.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("CUPOM_NAO_APLICAVEL", e.getCodigo());
    }

    @Test
    void validacao_formaPagamentoInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("INVALIDA");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", e.getCodigo());
    }

    @Test
    void validacao_parcelamentoInvalidoPix() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");
        req.setParcelas(2);

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void validacao_parcelamentoInvalidoCartao12Mais() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(13);

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void validacao_formaPagamentoIndisponivel_boletoAcimaLimite() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("1100"), 1, new BigDecimal("0.5"))
        ));
        req.setNivelClube("BRONZE");
        req.setRegiao("SUDESTE");
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("BOLETO");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> calculadora.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void fretegratisComCupom() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("1"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("FRETEGRATIS");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertEquals(new BigDecimal("100.00"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("29.50"), resposta.getFrete());
        assertEquals(new BigDecimal("29.50"), resposta.getDescontoCupom());
    }

    @Test
    void ouroComBrinde() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("501"), 1, new BigDecimal("1"))
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("OURO");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertTrue(resposta.getBrinde());
    }

    @Test
    void prataCreditoCalculado() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("1"))
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("PRATA");

        RespostaCheckout resposta = calculadora.calcular(req);

        assertEquals(new BigDecimal("2.00"), resposta.getCreditoProximaCompra());
    }
}
