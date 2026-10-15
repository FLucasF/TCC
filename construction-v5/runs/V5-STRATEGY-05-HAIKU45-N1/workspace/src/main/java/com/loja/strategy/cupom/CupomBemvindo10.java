package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import com.loja.util.RoundingUtil;
import java.math.BigDecimal;

public class CupomBemvindo10 implements CalculadoraCupom {
  @Override
  public BigDecimal calcular(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    return RoundingUtil.round(subtotalProdutos.multiply(new BigDecimal("0.10")));
  }

  @Override
  public boolean podeAplicar(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    return true;
  }
}
