package com.loja.checkout.strategy.cupom;

import com.loja.checkout.model.Item;
import java.util.List;

public interface EstrategiaCupom {
  double calcularDesconto(List<Item> itens, double subtotal);
  boolean aplicavel(List<Item> itens, double subtotal);
  boolean aplicaFreteGratis();
}
