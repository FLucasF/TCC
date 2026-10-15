package com.loja.strategy.entrega;

import com.loja.util.RoundingUtil;
import java.math.BigDecimal;

public class EntregaEconomica implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    return RoundingUtil.round(new BigDecimal("12.00").add(pesoTotal.multiply(new BigDecimal("2.00"))));
  }

  @Override
  public Integer getPrazo() {
    return 7;
  }

  @Override
  public boolean podeAplicar(BigDecimal pesoTotal) {
    return true;
  }
}
