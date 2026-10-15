package com.loja.checkout.strategy.cupom;

import com.loja.checkout.model.Item;
import com.loja.checkout.service.RoundingService;
import java.util.List;

public class BemVindo10 implements EstrategiaCupom {
  private RoundingService rounding;

  public BemVindo10(RoundingService rounding) {
    this.rounding = rounding;
  }

  @Override
  public double calcularDesconto(List<Item> itens, double subtotal) {
    return rounding.round(subtotal * 0.10);
  }

  @Override
  public boolean aplicavel(List<Item> itens, double subtotal) {
    return true;
  }

  @Override
  public boolean aplicaFreteGratis() {
    return false;
  }
}
