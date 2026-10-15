package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public interface CalculadoraCupom {
  BigDecimal calcular(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete);

  boolean podeAplicar(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
