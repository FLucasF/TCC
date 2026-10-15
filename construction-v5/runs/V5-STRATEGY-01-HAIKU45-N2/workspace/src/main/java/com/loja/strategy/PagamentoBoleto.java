package com.loja.strategy;

import java.math.BigDecimal;
import com.loja.util.Arredondador;

public class PagamentoBoleto implements CalculadoraPagamento {
  private static final BigDecimal TARIFA = new BigDecimal("3.49");

  @Override
  public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
    return TARIFA;
  }

  @Override
  public void validar(int parcelas, BigDecimal total) throws IllegalArgumentException {
    if (parcelas != 1) {
      throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
    }
    if (total.compareTo(new BigDecimal("1000.00")) > 0) {
      throw new IllegalArgumentException("FORMA_PAGAMENTO_INDISPONIVEL");
    }
  }

  @Override
  public BigDecimal calcularValorParcela(BigDecimal totalComAjuste, int parcelas) {
    return totalComAjuste;
  }
}
