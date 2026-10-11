package com.loja.strategy;

import java.math.BigDecimal;

public class EntregaMotoboy implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    return new BigDecimal("18.00");
  }

  @Override
  public int obterPrazoEntregaDias() {
    return 0;
  }

  @Override
  public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
    if (pesoTotal.compareTo(new BigDecimal("5")) > 0) {
      throw new IllegalArgumentException("MODALIDADE_INDISPONIVEL");
    }
  }
}
