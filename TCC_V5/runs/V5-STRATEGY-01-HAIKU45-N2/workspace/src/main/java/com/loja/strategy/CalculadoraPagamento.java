package com.loja.strategy;

import java.math.BigDecimal;

public interface CalculadoraPagamento {
  BigDecimal calcularAjuste(BigDecimal total, int parcelas);

  void validar(int parcelas, BigDecimal total) throws IllegalArgumentException;

  BigDecimal calcularValorParcela(BigDecimal totalComAjuste, int parcelas);
}
