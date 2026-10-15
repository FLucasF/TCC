package com.loja.checkout.strategy.cupom;

import com.loja.checkout.model.Item;
import java.util.List;

public class FreteGratis implements EstrategiaCupom {
  @Override
  public double calcularDesconto(List<Item> itens, double subtotal) {
    return 0.0;
  }

  @Override
  public boolean aplicavel(List<Item> itens, double subtotal) {
    return true;
  }

  @Override
  public boolean aplicaFreteGratis() {
    return true;
  }
}
