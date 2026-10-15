package com.loja.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemCarrinho;

public class ResumoCheckoutServiceTest {
  private ResumoCheckoutService service = new ResumoCheckoutService();

  @Test
  public void testExemplo1() {
    CheckoutRequest request = new CheckoutRequest();
    ItemCarrinho item1 = new ItemCarrinho();
    item1.nome = "Camiseta";
    item1.precoUnitario = 79.90;
    item1.quantidade = 2;
    item1.pesoKg = 0.30;

    ItemCarrinho item2 = new ItemCarrinho();
    item2.nome = "Tênis";
    item2.precoUnitario = 249.90;
    item2.quantidade = 1;
    item2.pesoKg = 1.20;

    request.itens = Arrays.asList(item1, item2);
    request.modalidadeEntrega = "EXPRESSA";
    request.cupom = "BEMVINDO10";
    request.formaPagamento = "PIX";
    request.parcelas = 1;
    request.nivelClube = "BRONZE";
    request.regiao = "NORTE";

    CheckoutResponse response = service.calcularResumo(request);

    assertEquals(new BigDecimal("409.70"), response.subtotalProdutos);
    assertEquals(new BigDecimal("40.97"), response.descontoCupom);
    assertEquals(new BigDecimal("33.10"), response.frete);
    assertEquals(2, response.prazoEntregaDias);
    assertEquals(new BigDecimal("10.24"), response.seguro);
    assertEquals(new BigDecimal("-20.60"), response.ajustePagamento);
    assertEquals(new BigDecimal("391.47"), response.totalFinal);
    assertEquals(1, response.parcelas);
    assertEquals(new BigDecimal("391.47"), response.valorParcela);
    assertEquals(0, response.creditoProximaCompra.compareTo(BigDecimal.ZERO));
    assertFalse(response.brinde);
  }

  @Test
  public void testExemplo2() {
    CheckoutRequest request = new CheckoutRequest();
    ItemCarrinho item1 = new ItemCarrinho();
    item1.nome = "Camiseta";
    item1.precoUnitario = 79.90;
    item1.quantidade = 2;
    item1.pesoKg = 0.30;

    ItemCarrinho item2 = new ItemCarrinho();
    item2.nome = "Tênis";
    item2.precoUnitario = 249.90;
    item2.quantidade = 1;
    item2.pesoKg = 1.20;

    request.itens = Arrays.asList(item1, item2);
    request.modalidadeEntrega = "ECONOMICA";
    request.cupom = null;
    request.formaPagamento = "CARTAO";
    request.parcelas = 6;
    request.nivelClube = "PRATA";
    request.regiao = "CENTRO_OESTE";

    CheckoutResponse response = service.calcularResumo(request);

    assertEquals(new BigDecimal("409.70"), response.subtotalProdutos);
    assertEquals(new BigDecimal("0.00"), response.descontoCupom);
    assertEquals(new BigDecimal("15.60"), response.frete);
    assertEquals(7, response.prazoEntregaDias);
    assertEquals(new BigDecimal("6.15"), response.seguro);
    assertEquals(new BigDecimal("30.55"), response.ajustePagamento);
    assertEquals(new BigDecimal("462.00"), response.totalFinal);
    assertEquals(6, response.parcelas);
    assertEquals(new BigDecimal("77.00"), response.valorParcela);
    assertEquals(0, response.creditoProximaCompra.compareTo(new BigDecimal("8.19")));
    assertFalse(response.brinde);
  }

  @Test
  public void testExemplo3() {
    CheckoutRequest request = new CheckoutRequest();
    ItemCarrinho item1 = new ItemCarrinho();
    item1.nome = "Fone";
    item1.precoUnitario = 199.90;
    item1.quantidade = 2;
    item1.pesoKg = 0.25;

    request.itens = Arrays.asList(item1);
    request.modalidadeEntrega = "MOTOBOY";
    request.cupom = "MENOS50";
    request.formaPagamento = "BOLETO";
    request.parcelas = 1;
    request.nivelClube = "BRONZE";
    request.regiao = "NORDESTE";

    CheckoutResponse response = service.calcularResumo(request);

    assertEquals(new BigDecimal("399.80"), response.subtotalProdutos);
    assertEquals(new BigDecimal("50.00"), response.descontoCupom);
    assertEquals(new BigDecimal("18.00"), response.frete);
    assertEquals(0, response.prazoEntregaDias);
    assertEquals(new BigDecimal("8.00"), response.seguro);
    assertEquals(new BigDecimal("3.49"), response.ajustePagamento);
    assertEquals(new BigDecimal("379.29"), response.totalFinal);
    assertEquals(1, response.parcelas);
    assertEquals(new BigDecimal("379.29"), response.valorParcela);
    assertEquals(0, response.creditoProximaCompra.compareTo(BigDecimal.ZERO));
    assertFalse(response.brinde);
  }

  @Test
  public void testExemplo4() {
    CheckoutRequest request = new CheckoutRequest();
    ItemCarrinho item1 = new ItemCarrinho();
    item1.nome = "Meia";
    item1.precoUnitario = 19.90;
    item1.quantidade = 7;
    item1.pesoKg = 0.10;

    ItemCarrinho item2 = new ItemCarrinho();
    item2.nome = "Camiseta";
    item2.precoUnitario = 79.90;
    item2.quantidade = 2;
    item2.pesoKg = 0.30;

    request.itens = Arrays.asList(item1, item2);
    request.modalidadeEntrega = "RETIRADA_LOJA";
    request.cupom = "LEVE3PAGUE2";
    request.formaPagamento = "CARTAO";
    request.parcelas = 3;
    request.nivelClube = "PRATA";
    request.regiao = "SUL";

    CheckoutResponse response = service.calcularResumo(request);

    assertEquals(new BigDecimal("299.10"), response.subtotalProdutos);
    assertEquals(new BigDecimal("39.80"), response.descontoCupom);
    assertEquals(0, response.frete.compareTo(BigDecimal.ZERO));
    assertEquals(1, response.prazoEntregaDias);
    assertEquals(new BigDecimal("2.99"), response.seguro);
    assertEquals(0, response.ajustePagamento.compareTo(BigDecimal.ZERO));
    assertEquals(new BigDecimal("262.29"), response.totalFinal);
    assertEquals(3, response.parcelas);
    assertEquals(new BigDecimal("87.43"), response.valorParcela);
    assertEquals(0, response.creditoProximaCompra.compareTo(new BigDecimal("5.98")));
    assertFalse(response.brinde);
  }

  @Test
  public void testExemplo5() {
    CheckoutRequest request = new CheckoutRequest();
    ItemCarrinho item1 = new ItemCarrinho();
    item1.nome = "Camiseta";
    item1.precoUnitario = 79.90;
    item1.quantidade = 2;
    item1.pesoKg = 0.30;

    ItemCarrinho item2 = new ItemCarrinho();
    item2.nome = "Tênis";
    item2.precoUnitario = 249.90;
    item2.quantidade = 1;
    item2.pesoKg = 1.20;

    request.itens = Arrays.asList(item1, item2);
    request.modalidadeEntrega = "EXPRESSA";
    request.cupom = null;
    request.formaPagamento = "PIX";
    request.parcelas = 1;
    request.nivelClube = "OURO";
    request.regiao = "SUDESTE";

    CheckoutResponse response = service.calcularResumo(request);

    assertEquals(new BigDecimal("409.70"), response.subtotalProdutos);
    assertEquals(0, response.descontoCupom.compareTo(BigDecimal.ZERO));
    assertEquals(0, response.frete.compareTo(BigDecimal.ZERO));
    assertEquals(2, response.prazoEntregaDias);
    assertEquals(new BigDecimal("4.10"), response.seguro);
    assertEquals(new BigDecimal("-20.69"), response.ajustePagamento);
    assertEquals(new BigDecimal("393.11"), response.totalFinal);
    assertEquals(1, response.parcelas);
    assertEquals(new BigDecimal("393.11"), response.valorParcela);
    assertEquals(0, response.creditoProximaCompra.compareTo(new BigDecimal("20.48")));
    assertFalse(response.brinde);
  }
}
