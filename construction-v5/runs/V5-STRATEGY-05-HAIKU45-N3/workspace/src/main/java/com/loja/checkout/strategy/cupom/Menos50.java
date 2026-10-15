package com.loja.checkout.strategy.cupom;

import com.loja.checkout.model.Item;
import java.util.List;

public class Menos50 implements EstrategiaCupom {
  @Override
  public double calcularDesconto(List<Item> itens, double subtotal) {
    return 50.0;
  }

  @Override
  public boolean aplicavel(List<Item> itens, double subtotal) {
    return subtotal >= 300.0;
  }

  @Override
  public boolean aplicaFreteGratis() {
    return false;
  }
}
