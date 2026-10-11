package com.loja.strategy;

import java.math.BigDecimal;
import com.loja.util.Arredondador;

public class EntregaExpressa implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    BigDecimal base = new BigDecimal("25.00");
    BigDecimal porKg = pesoTotal.multiply(new BigDecimal("4.50"));
    return Arredondador.arredondar(base.add(porKg));
  }

  @Override
  public int obterPrazoEntregaDias() {
    return 2;
  }

  @Override
  public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
  }
}
