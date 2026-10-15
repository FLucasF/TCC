package com.loja.strategy.entrega;

import java.math.BigDecimal;

public class EntregaMotoboy implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    return new BigDecimal("18.00");
  }

  @Override
  public Integer getPrazo() {
    return 0;
  }

  @Override
  public boolean podeAplicar(BigDecimal pesoTotal) {
    return pesoTotal.compareTo(new BigDecimal("5.00")) <= 0;
  }
}
