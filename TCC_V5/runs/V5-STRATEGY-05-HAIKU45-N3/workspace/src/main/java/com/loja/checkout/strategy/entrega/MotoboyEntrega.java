package com.loja.checkout.strategy.entrega;

import java.util.List;
import com.loja.checkout.model.Item;

public class MotoboyEntrega implements EstrategiaEntrega {
  @Override
  public double calcularFrete(List<Item> itens) {
    return 18.0;
  }

  @Override
  public int prazoEntregaDias() {
    return 0;
  }

  @Override
  public boolean disponivel(List<Item> itens) {
    double pesoTotal = itens.stream()
        .mapToDouble(item -> item.pesoKg * item.quantidade)
        .sum();
    return pesoTotal <= 5.0;
  }
}
