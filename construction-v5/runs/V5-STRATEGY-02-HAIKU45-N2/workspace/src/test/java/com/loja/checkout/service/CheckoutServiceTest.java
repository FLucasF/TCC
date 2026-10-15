package com.loja.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.model.request.CheckoutRequest;
import com.loja.checkout.model.request.Item;
import com.loja.checkout.model.response.CheckoutResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {
  private CheckoutService service;

  @BeforeEach
  void setUp() {
    service = new CheckoutService();
  }

  private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
    if (expected.compareTo(actual) != 0) {
      assertEquals(expected.toPlainString(), actual.toPlainString());
    }
  }

  @Test
  void exemplo1() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
        new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
    ));
    request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
    request.setCupom("BEMVINDO10");
    request.setFormaPagamento(FormaPagamento.PIX);
    request.setParcelas(1);
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORTE);

    CheckoutResponse response = service.calcular(request);

    assertBigDecimalEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
    assertBigDecimalEquals(new BigDecimal("40.97"), response.getDescontoCupom());
    assertBigDecimalEquals(new BigDecimal("33.10"), response.getFrete());
    assertEquals(2, response.getPrazoEntregaDias());
    assertBigDecimalEquals(new BigDecimal("10.24"), response.getSeguro());
    assertBigDecimalEquals(new BigDecimal("-20.60"), response.getAjustePagamento());
    assertBigDecimalEquals(new BigDecimal("391.47"), response.getTotalFinal());
    assertEquals(1, response.getParcelas());
    assertBigDecimalEquals(new BigDecimal("391.47"), response.getValorParcela());
    assertBigDecimalEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
    assertEquals(false, response.getBrinde());
  }

  @Test
  void exemplo2() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
        new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
    ));
    request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
    request.setCupom(null);
    request.setFormaPagamento(FormaPagamento.CARTAO);
    request.setParcelas(6);
    request.setNivelClube(NivelClube.PRATA);
    request.setRegiao(Regiao.CENTRO_OESTE);

    CheckoutResponse response = service.calcular(request);

    assertBigDecimalEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
    assertBigDecimalEquals(new BigDecimal("0.00"), response.getDescontoCupom());
    assertBigDecimalEquals(new BigDecimal("15.60"), response.getFrete());
    assertEquals(7, response.getPrazoEntregaDias());
    assertBigDecimalEquals(new BigDecimal("6.15"), response.getSeguro());
    assertBigDecimalEquals(new BigDecimal("30.55"), response.getAjustePagamento());
    assertBigDecimalEquals(new BigDecimal("462.00"), response.getTotalFinal());
    assertEquals(6, response.getParcelas());
    assertBigDecimalEquals(new BigDecimal("77.00"), response.getValorParcela());
    assertBigDecimalEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
    assertEquals(false, response.getBrinde());
  }

  @Test
  void exemplo3() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.singletonList(
        new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
    ));
    request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
    request.setCupom("MENOS50");
    request.setFormaPagamento(FormaPagamento.BOLETO);
    request.setParcelas(1);
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORDESTE);

    CheckoutResponse response = service.calcular(request);

    assertBigDecimalEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
    assertBigDecimalEquals(new BigDecimal("50.00"), response.getDescontoCupom());
    assertBigDecimalEquals(new BigDecimal("18.00"), response.getFrete());
    assertEquals(0, response.getPrazoEntregaDias());
    assertBigDecimalEquals(new BigDecimal("8.00"), response.getSeguro());
    assertBigDecimalEquals(new BigDecimal("3.49"), response.getAjustePagamento());
    assertBigDecimalEquals(new BigDecimal("379.29"), response.getTotalFinal());
    assertEquals(1, response.getParcelas());
    assertBigDecimalEquals(new BigDecimal("379.29"), response.getValorParcela());
    assertBigDecimalEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
    assertEquals(false, response.getBrinde());
  }

  @Test
  void exemplo4() {
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

    assertBigDecimalEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
    assertBigDecimalEquals(new BigDecimal("39.80"), response.getDescontoCupom());
    assertBigDecimalEquals(new BigDecimal("0.00"), response.getFrete());
    assertEquals(1, response.getPrazoEntregaDias());
    assertBigDecimalEquals(new BigDecimal("2.99"), response.getSeguro());
    assertBigDecimalEquals(new BigDecimal("0.00"), response.getAjustePagamento());
    assertBigDecimalEquals(new BigDecimal("262.29"), response.getTotalFinal());
    assertEquals(3, response.getParcelas());
    assertBigDecimalEquals(new BigDecimal("87.43"), response.getValorParcela());
    assertBigDecimalEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
    assertEquals(false, response.getBrinde());
  }

  @Test
  void exemplo5() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
        new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
    ));
    request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
    request.setCupom(null);
    request.setFormaPagamento(FormaPagamento.PIX);
    request.setParcelas(1);
    request.setNivelClube(NivelClube.OURO);
    request.setRegiao(Regiao.SUDESTE);

    CheckoutResponse response = service.calcular(request);

    assertBigDecimalEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
    assertBigDecimalEquals(new BigDecimal("0.00"), response.getDescontoCupom());
    assertBigDecimalEquals(new BigDecimal("0.00"), response.getFrete());
    assertEquals(2, response.getPrazoEntregaDias());
    assertBigDecimalEquals(new BigDecimal("4.10"), response.getSeguro());
    assertBigDecimalEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
    assertBigDecimalEquals(new BigDecimal("393.11"), response.getTotalFinal());
    assertEquals(1, response.getParcelas());
    assertBigDecimalEquals(new BigDecimal("393.11"), response.getValorParcela());
    assertBigDecimalEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
    assertEquals(false, response.getBrinde());
  }

  @Test
  void pedidoInvalido() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.emptyList());
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORTE);
    request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
    request.setFormaPagamento(FormaPagamento.PIX);

    assertThrows(CheckoutService.ValidationException.class, () -> service.calcular(request));
  }

  @Test
  void nivelClubeInvalido() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.singletonList(
        new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.10"))
    ));
    request.setNivelClube(null);
    request.setRegiao(Regiao.NORTE);
    request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
    request.setFormaPagamento(FormaPagamento.PIX);

    assertThrows(CheckoutService.ValidationException.class, () -> service.calcular(request));
  }

  @Test
  void motoboy5kg() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.singletonList(
        new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("6.00"))
    ));
    request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORTE);
    request.setFormaPagamento(FormaPagamento.PIX);

    assertThrows(CheckoutService.ValidationException.class, () -> service.calcular(request));
  }

  @Test
  void cupomInvalido() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.singletonList(
        new Item("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.10"))
    ));
    request.setCupom("CUPOM_INEXISTENTE");
    request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORTE);
    request.setFormaPagamento(FormaPagamento.PIX);

    assertThrows(CheckoutService.ValidationException.class, () -> service.calcular(request));
  }

  @Test
  void cupomMenos50NaoAplicavel() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.singletonList(
        new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.10"))
    ));
    request.setCupom("MENOS50");
    request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORTE);
    request.setFormaPagamento(FormaPagamento.PIX);

    assertThrows(CheckoutService.ValidationException.class, () -> service.calcular(request));
  }

  @Test
  void parcelamentoPixInvalido() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.singletonList(
        new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.10"))
    ));
    request.setFormaPagamento(FormaPagamento.PIX);
    request.setParcelas(2);
    request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORTE);

    assertThrows(CheckoutService.ValidationException.class, () -> service.calcular(request));
  }

  @Test
  void boletoMaiorQueMil() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.singletonList(
        new Item("Produto", new BigDecimal("1050.00"), 1, new BigDecimal("1.00"))
    ));
    request.setFormaPagamento(FormaPagamento.BOLETO);
    request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
    request.setNivelClube(NivelClube.BRONZE);
    request.setRegiao(Regiao.NORTE);

    assertThrows(CheckoutService.ValidationException.class, () -> service.calcular(request));
  }
}
