package com.loja.checkout.service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import com.loja.checkout.exception.ErroCheckout;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void exemplo1() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("BEMVINDO10");
        req.setFormaPagamento("PIX");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        RespostaCheckout resp = checkoutService.calcularResumo(req);

        assertEquals(new BigDecimal("409.70"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resp.getFrete());
        assertEquals(2, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resp.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(new BigDecimal("391.47"), resp.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resp.getCreditoProximaCompra());
        assertFalse(resp.getBrinde());
    }

    @Test
    void exemplo2() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        req.setModalidadeEntrega("ECONOMICA");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(6);
        req.setNivelClube("PRATA");
        req.setRegiao("CENTRO_OESTE");

        RespostaCheckout resp = checkoutService.calcularResumo(req);

        assertEquals(new BigDecimal("409.70"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resp.getFrete());
        assertEquals(7, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resp.getSeguro());
        assertEquals(new BigDecimal("30.55"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resp.getTotalFinal());
        assertEquals(6, resp.getParcelas());
        assertEquals(new BigDecimal("77.00"), resp.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resp.getCreditoProximaCompra());
        assertFalse(resp.getBrinde());
    }

    @Test
    void exemplo3() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setCupom("MENOS50");
        req.setFormaPagamento("BOLETO");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORDESTE");

        RespostaCheckout resp = checkoutService.calcularResumo(req);

        assertEquals(new BigDecimal("399.80"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resp.getFrete());
        assertEquals(0, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resp.getSeguro());
        assertEquals(new BigDecimal("3.49"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(new BigDecimal("379.29"), resp.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resp.getCreditoProximaCompra());
        assertFalse(resp.getBrinde());
    }

    @Test
    void exemplo4() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setCupom("LEVE3PAGUE2");
        req.setFormaPagamento("CARTAO");
        req.setParcelas(3);
        req.setNivelClube("PRATA");
        req.setRegiao("SUL");

        RespostaCheckout resp = checkoutService.calcularResumo(req);

        assertEquals(new BigDecimal("299.10"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.getFrete());
        assertEquals(1, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resp.getSeguro());
        assertEquals(new BigDecimal("0.00"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resp.getTotalFinal());
        assertEquals(3, resp.getParcelas());
        assertEquals(new BigDecimal("87.43"), resp.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resp.getCreditoProximaCompra());
        assertFalse(resp.getBrinde());
    }

    @Test
    void exemplo5() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setParcelas(1);
        req.setNivelClube("OURO");
        req.setRegiao("SUDESTE");

        RespostaCheckout resp = checkoutService.calcularResumo(req);

        assertEquals(new BigDecimal("409.70"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.getFrete());
        assertEquals(2, resp.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resp.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resp.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resp.getTotalFinal());
        assertEquals(1, resp.getParcelas());
        assertEquals(new BigDecimal("393.11"), resp.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resp.getCreditoProximaCompra());
        assertFalse(resp.getBrinde());
    }

    @Test
    void pedidoInvalido_carrinhVazio() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList());
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
    }

    @Test
    void nivelClubeInvalido() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("INVALIDO");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("NIVEL_CLUBE_INVALIDO", erro.getCodigo());
    }

    @Test
    void regiaoinvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("INVALIDA");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("REGIAO_INVALIDA", erro.getCodigo());
    }

    @Test
    void modalidadeInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("INVALIDA");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigo());
    }

    @Test
    void modalidadeIndisponivel_motoboySobreExcessoDePeso() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 20, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("MOTOBOY");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("MODALIDADE_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void cupomInvalido() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("INVALIDO");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("CUPOM_INVALIDO", erro.getCodigo());
    }

    @Test
    void cupomNaoAplicavel_MENOS50() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("MENOS50");
        req.setFormaPagamento("PIX");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("CUPOM_NAO_APLICAVEL", erro.getCodigo());
    }

    @Test
    void formaPagamentoInvalida() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("INVALIDA");
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigo());
    }

    @Test
    void parcelamentoInvalido_pixComParcelas() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setFormaPagamento("PIX");
        req.setParcelas(2);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcimaDeUmMil() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Televisão", new BigDecimal("1500.00"), 1, new BigDecimal("5.00"))
        ));
        req.setModalidadeEntrega("RETIRADA_LOJA");
        req.setFormaPagamento("BOLETO");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> checkoutService.calcularResumo(req));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void cupomFrGratis() {
        RequisicaoCheckout req = new RequisicaoCheckout();
        req.setItens(Arrays.asList(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        req.setModalidadeEntrega("EXPRESSA");
        req.setCupom("FRETEGRATIS");
        req.setFormaPagamento("PIX");
        req.setParcelas(1);
        req.setNivelClube("BRONZE");
        req.setRegiao("NORTE");

        RespostaCheckout resp = checkoutService.calcularResumo(req);

        assertEquals(new BigDecimal("409.70"), resp.getSubtotalProdutos());
        assertEquals(new BigDecimal("33.10"), resp.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resp.getFrete());
    }
}
