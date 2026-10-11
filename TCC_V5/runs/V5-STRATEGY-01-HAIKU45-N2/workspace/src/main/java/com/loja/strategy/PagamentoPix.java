package com.loja.strategy;

import java.math.BigDecimal;
import com.loja.util.Arredondador;

public class PagamentoPix implements CalculadoraPagamento {
  @Override
  public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
    BigDecimal desconto = Arredondador.arredondar(total.multiply(new BigDecimal("0.05")));
    return desconto.negate();
  }

  @Override
  public void validar(int parcelas, BigDecimal total) throws IllegalArgumentException {
    if (parcelas != 1) {
      throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
    }
  }

  @Override
  public BigDecimal calcularValorParcela(BigDecimal totalComAjuste, int parcelas) {
    return totalComAjuste;
  }
}
