package com.loja.checkout.strategy.entrega;

import java.util.List;
import com.loja.checkout.model.Item;
import com.loja.checkout.service.RoundingService;

public class ExpressaEntrega implements EstrategiaEntrega {
  private RoundingService rounding;

  public ExpressaEntrega(RoundingService rounding) {
    this.rounding = rounding;
  }

  @Override
  public double calcularFrete(List<Item> itens) {
    double pesoTotal = itens.stream()
        .mapToDouble(item -> item.pesoKg * item.quantidade)
        .sum();
    return rounding.round(25.0 + 4.5 * pesoTotal);
  }

  @Override
  public int prazoEntregaDias() {
    return 2;
  }

  @Override
  public boolean disponivel(List<Item> itens) {
    return true;
  }
}
