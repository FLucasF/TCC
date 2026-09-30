package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutServiceTest {

  @Autowired
  private CheckoutService checkoutService;

  @Test
  void exemplo1() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
        new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setCupom("BEMVINDO10");
    request.setFormaPagamento("PIX");
    request.setParcelas(1);
    request.setNivelClube("BRONZE");
    request.setRegiao(null);

    CheckoutResponse response = checkoutService.calcularResumo(request);

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
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
        new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
    ));
    request.setModalidadeEntrega("ECONOMICA");
    request.setCupom(null);
    request.setFormaPagamento("CARTAO");
    request.setParcelas(6);
    request.setNivelClube("BRONZE");
    request.setRegiao(null);

    CheckoutResponse response = checkoutService.calcularResumo(request);

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
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
    ));
    request.setModalidadeEntrega("MOTOBOY");
    request.setCupom("MENOS50");
    request.setFormaPagamento("BOLETO");
    request.setParcelas(1);
    request.setNivelClube("BRONZE");
    request.setRegiao(null);

    CheckoutResponse response = checkoutService.calcularResumo(request);

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
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
    ));
    request.setModalidadeEntrega("RETIRADA_LOJA");
    request.setCupom("LEVE3PAGUE2");
    request.setFormaPagamento("CARTAO");
    request.setParcelas(3);
    request.setNivelClube("BRONZE");
    request.setRegiao(null);

    CheckoutResponse response = checkoutService.calcularResumo(request);

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
  void exemplo5() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
        new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setCupom(null);
    request.setFormaPagamento("PIX");
    request.setParcelas(1);
    request.setNivelClube("OURO");
    request.setRegiao("SUDESTE");

    CheckoutResponse response = checkoutService.calcularResumo(request);

    assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
    assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
    assertEquals(new BigDecimal("0.00"), response.getFrete());
    assertEquals(2, response.getPrazoEntregaDias());
    assertEquals(new BigDecimal("49.16"), response.getImposto());
    assertEquals(new BigDecimal("-22.94"), response.getAjustePagamento());
    assertEquals(new BigDecimal("435.92"), response.getTotalFinal());
    assertEquals(1, response.getParcelas());
    assertEquals(new BigDecimal("435.92"), response.getValorParcela());
    assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
    assertFalse(response.getBrinde());
  }

  @Test
  void erro1_CarrinhoVazio() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Collections.emptyList());
    request.setModalidadeEntrega("EXPRESSA");
    request.setFormaPagamento("PIX");
    request.setNivelClube("BRONZE");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
  }

  @Test
  void erro2_NivelClubeInvalido() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setFormaPagamento("PIX");
    request.setNivelClube("INVALIDO");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigo());
  }

  @Test
  void erro3_RegiaoInvalida() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setFormaPagamento("PIX");
    request.setNivelClube("BRONZE");
    request.setRegiao("INVALIDA");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("REGIAO_INVALIDA", exception.getCodigo());
  }

  @Test
  void erro4_ModalidadeInvalida() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("INVALIDA");
    request.setFormaPagamento("PIX");
    request.setNivelClube("BRONZE");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
  }

  @Test
  void erro5_ModalidadeIndisponivel() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("6.0"))
    ));
    request.setModalidadeEntrega("MOTOBOY");
    request.setFormaPagamento("PIX");
    request.setNivelClube("BRONZE");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
  }

  @Test
  void erro6_CupomInvalido() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setFormaPagamento("PIX");
    request.setNivelClube("BRONZE");
    request.setCupom("CUPOM_INEXISTENTE");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("CUPOM_INVALIDO", exception.getCodigo());
  }

  @Test
  void erro7_CupomNaoAplicavel() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setFormaPagamento("PIX");
    request.setNivelClube("BRONZE");
    request.setCupom("MENOS50");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
  }

  @Test
  void erro8_FormaPagamentoInvalida() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setFormaPagamento("INVALIDA");
    request.setNivelClube("BRONZE");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
  }

  @Test
  void erro9_ParcelamentoInvalido_PixComMaisDe1Parcela() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("100"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("EXPRESSA");
    request.setFormaPagamento("PIX");
    request.setNivelClube("BRONZE");
    request.setParcelas(2);

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
  }

  @Test
  void erro10_FormaPagamentoIndisponivel_BoletoAcima1000() {
    CheckoutRequest request = new CheckoutRequest();
    request.setItens(Arrays.asList(
        new ItemRequest("Produto", new BigDecimal("1001"), 1, new BigDecimal("0.5"))
    ));
    request.setModalidadeEntrega("RETIRADA_LOJA");
    request.setFormaPagamento("BOLETO");
    request.setNivelClube("BRONZE");

    CheckoutException exception = assertThrows(CheckoutException.class, () -> checkoutService.calcularResumo(request));
    assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
  }
}
