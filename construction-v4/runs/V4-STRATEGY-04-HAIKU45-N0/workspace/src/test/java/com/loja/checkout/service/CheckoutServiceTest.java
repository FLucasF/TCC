package com.loja.checkout.service;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.Item;
import com.loja.checkout.model.dto.CheckoutRequest;
import com.loja.checkout.model.dto.CheckoutResponse;
import com.loja.checkout.model.enums.FormaPagamento;
import com.loja.checkout.model.enums.ModalidadeEntrega;
import com.loja.checkout.model.enums.NivelClube;
import com.loja.checkout.model.enums.Regiao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaBemvindo10PixNorte() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        CheckoutResponse response = service.calcular(request);

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
        assertFalse(response.isBrinde());
    }

    @Test
    void exemplo2_CamisetaTenisEconomicaCartao6xPratacentroOeste() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(6);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.CENTRO_OESTE);

        CheckoutResponse response = service.calcular(request);

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
        assertFalse(response.isBrinde());
    }

    @Test
    void exemplo3_FoneMotoboymenos50BoletoNordeste() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORDESTE);

        CheckoutResponse response = service.calcular(request);

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
        assertFalse(response.isBrinde());
    }

    @Test
    void exemplo4_MeiacamisetaRetiradaLojaLeve3Pague2Cartao3xSul() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(3);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.SUL);

        CheckoutResponse response = service.calcular(request);

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
        assertFalse(response.isBrinde());
    }

    @Test
    void exemplo5_CamisetaTenisExpressaPixOuroSudeste() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.OURO);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutResponse response = service.calcular(request);

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
        assertFalse(response.isBrinde());
    }

    @Test
    void validacao_carrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.emptyList());
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_itemComPrecoNegativo() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("-10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_nivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(null);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_regiao_invalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(null);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("REGIAO_INVALIDA", exception.getCodigo());
    }

    @Test
    void validacao_modalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(null);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void validacao_motoboyAcima5kg() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("5.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void validacao_cupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setCupom("CUPOM_INEXISTENTE");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_menos50Abaixo300() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void validacao_formaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(null);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void validacao_parcelamentoInvalidoPixComParcelas() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(2);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_boletoAcima1000() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("1200.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void validacao_cardinaldadeParcelas() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.50"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(13);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }
}
