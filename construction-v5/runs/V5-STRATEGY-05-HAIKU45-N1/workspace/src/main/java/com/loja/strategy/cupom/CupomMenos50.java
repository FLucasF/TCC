package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public class CupomMenos50 implements CalculadoraCupom {
  @Override
  public BigDecimal calcular(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    return new BigDecimal("50.00");
  }

  @Override
  public boolean podeAplicar(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
  }
}
