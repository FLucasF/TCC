package com.loja.checkout.strategy.entrega;

import java.util.List;
import com.loja.checkout.model.Item;

public interface EstrategiaEntrega {
  double calcularFrete(List<Item> itens);
  int prazoEntregaDias();
  boolean disponivel(List<Item> itens);
}
