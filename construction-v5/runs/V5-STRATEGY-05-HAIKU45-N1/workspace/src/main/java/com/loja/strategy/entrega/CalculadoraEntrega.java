package com.loja.strategy.entrega;

import java.math.BigDecimal;

public interface CalculadoraEntrega {
  BigDecimal calcularFrete(BigDecimal pesoTotal);

  Integer getPrazo();

  boolean podeAplicar(BigDecimal pesoTotal);
}
