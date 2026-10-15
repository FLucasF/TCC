package com.loja.strategy.entrega;

import com.loja.util.RoundingUtil;
import java.math.BigDecimal;

public class EntregaExpressa implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    return RoundingUtil.round(new BigDecimal("25.00").add(pesoTotal.multiply(new BigDecimal("4.50"))));
  }

  @Override
  public Integer getPrazo() {
    return 2;
  }

  @Override
  public boolean podeAplicar(BigDecimal pesoTotal) {
    return true;
  }
}
