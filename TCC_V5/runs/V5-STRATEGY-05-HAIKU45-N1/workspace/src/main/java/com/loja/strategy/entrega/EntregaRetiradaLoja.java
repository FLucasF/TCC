package com.loja.strategy.entrega;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    return BigDecimal.ZERO;
  }

  @Override
  public Integer getPrazo() {
    return 1;
  }

  @Override
  public boolean podeAplicar(BigDecimal pesoTotal) {
    return true;
  }
}
