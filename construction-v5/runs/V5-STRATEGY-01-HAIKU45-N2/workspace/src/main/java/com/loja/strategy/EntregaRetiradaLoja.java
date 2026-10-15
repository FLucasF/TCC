package com.loja.strategy;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    return BigDecimal.ZERO;
  }

  @Override
  public int obterPrazoEntregaDias() {
    return 1;
  }

  @Override
  public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
  }
}
