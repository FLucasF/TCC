package com.loja.strategy;

import java.math.BigDecimal;

public interface CalculadoraEntrega {
  BigDecimal calcularFrete(BigDecimal pesoTotal);

  int obterPrazoEntregaDias();

  void validar(BigDecimal pesoTotal) throws IllegalArgumentException;
}
