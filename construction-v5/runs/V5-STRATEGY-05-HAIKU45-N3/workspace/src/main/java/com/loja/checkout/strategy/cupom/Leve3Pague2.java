package com.loja.checkout.strategy.cupom;

import com.loja.checkout.model.Item;
import com.loja.checkout.service.RoundingService;
import java.util.List;

public class Leve3Pague2 implements EstrategiaCupom {
  private RoundingService rounding;

  public Leve3Pague2(RoundingService rounding) {
    this.rounding = rounding;
  }

  @Override
  public double calcularDesconto(List<Item> itens, double subtotal) {
    double desconto = 0.0;
    for (Item item : itens) {
      int unidadesGratis = item.quantidade / 3;
      desconto += unidadesGratis * item.precoUnitario;
    }
    return rounding.round(desconto);
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
