package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public class CupomFreteGratis implements CalculadoraCupom {
  @Override
  public BigDecimal calcular(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    return frete;
  }

  @Override
  public boolean podeAplicar(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    return true;
  }
}
