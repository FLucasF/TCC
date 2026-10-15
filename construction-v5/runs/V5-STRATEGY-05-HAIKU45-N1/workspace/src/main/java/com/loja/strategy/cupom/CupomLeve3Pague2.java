package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import com.loja.util.RoundingUtil;
import java.math.BigDecimal;

public class CupomLeve3Pague2 implements CalculadoraCupom {
  @Override
  public BigDecimal calcular(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    BigDecimal totalComDesconto = BigDecimal.ZERO;
    for (CheckoutRequest.Item item : itens) {
      int gratuitas = item.getQuantidade() / 3;
      int pagas = item.getQuantidade() - gratuitas;
      totalComDesconto = totalComDesconto.add(item.getPrecoUnitario().multiply(new BigDecimal(pagas)));
    }
    totalComDesconto = RoundingUtil.round(totalComDesconto);
    return RoundingUtil.round(subtotalProdutos.subtract(totalComDesconto));
  }

  @Override
  public boolean podeAplicar(CheckoutRequest.Item[] itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    return true;
  }
}
