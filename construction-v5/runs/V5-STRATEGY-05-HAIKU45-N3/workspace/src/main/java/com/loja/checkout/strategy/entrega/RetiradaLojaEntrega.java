package com.loja.checkout.strategy.entrega;

import java.util.List;
import com.loja.checkout.model.Item;

public class RetiradaLojaEntrega implements EstrategiaEntrega {
  @Override
  public double calcularFrete(List<Item> itens) {
    return 0.0;
  }

  @Override
  public int prazoEntregaDias() {
    return 1;
  }

  @Override
  public boolean disponivel(List<Item> itens) {
    return true;
  }
}
