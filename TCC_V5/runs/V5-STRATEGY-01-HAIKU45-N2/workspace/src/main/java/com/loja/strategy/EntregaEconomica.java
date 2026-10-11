package com.loja.strategy;

import java.math.BigDecimal;
import com.loja.util.Arredondador;

public class EntregaEconomica implements CalculadoraEntrega {
  @Override
  public BigDecimal calcularFrete(BigDecimal pesoTotal) {
    BigDecimal base = new BigDecimal("12.00");
    BigDecimal porKg = pesoTotal.multiply(new BigDecimal("2.00"));
    return Arredondador.arredondar(base.add(porKg));
  }

  @Override
  public int obterPrazoEntregaDias() {
    return 7;
  }

  @Override
  public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
  }
}
