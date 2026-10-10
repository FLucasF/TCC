package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ResumoCheckoutServiceTest {

    private final ResumoCheckoutService service = new ResumoCheckoutService();

    @Test
    public void testExemplo1() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

        ResumoResponse response = service.calcularResumo(request);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo2() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(6);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.CENTRO_OESTE);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.67"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.12"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.02"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo3() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORDESTE);

        ResumoResponse response = service.calcularResumo(request);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo4() {
        ResumoRequest request = new ResumoRequest();
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

        ResumoResponse response = service.calcularResumo(request);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo5() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.OURO);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testCarrinhoVazio() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList());
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("PEDIDO_INVALIDO", response.getErro());
    }

    @Test
    public void testPrecoZero() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("0"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("PEDIDO_INVALIDO", response.getErro());
    }

    @Test
    public void testQuantidadeZero() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 0, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("PEDIDO_INVALIDO", response.getErro());
    }

    @Test
    public void testPesoNegativo() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("-1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("PEDIDO_INVALIDO", response.getErro());
    }

    @Test
    public void testNivelClubeInvalido() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(null);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("NIVEL_CLUBE_INVALIDO", response.getErro());
    }

    @Test
    public void testRegiaoInvalida() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(null);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("REGIAO_INVALIDA", response.getErro());
    }

    @Test
    public void testModalidadeInvalida() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(null);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("MODALIDADE_INVALIDA", response.getErro());
    }

    @Test
    public void testMotoboySobreweight() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 10, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("MODALIDADE_INDISPONIVEL", response.getErro());
    }

    @Test
    public void testCupomInvalido() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("CUPOM_INVALIDO", response.getErro());
    }

    @Test
    public void testCupomMenos50NaoAplicavel() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("CUPOM_NAO_APLICAVEL", response.getErro());
    }

    @Test
    public void testFormaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(null);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", response.getErro());
    }

    @Test
    public void testPixParcelamento() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(2);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("PARCELAMENTO_INVALIDO", response.getErro());
    }

    @Test
    public void testBoletoParcelamento() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setParcelas(2);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("PARCELAMENTO_INVALIDO", response.getErro());
    }

    @Test
    public void testCartaoParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(13);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("PARCELAMENTO_INVALIDO", response.getErro());
    }

    @Test
    public void testBoletoAcima1000() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("1001.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", response.getErro());
    }

    @Test
    public void testOuroBrinde() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("250.00"), 2, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.OURO);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals(true, response.getBrinde());
    }

    @Test
    public void testFreteGratis() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(new BigDecimal("29.50"), response.getDescontoCupom());
    }

    @Test
    public void testParcelesSemValor() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        ResumoResponse response = service.calcularResumo(request);
        assertEquals(1, response.getParcelas());
    }
}
