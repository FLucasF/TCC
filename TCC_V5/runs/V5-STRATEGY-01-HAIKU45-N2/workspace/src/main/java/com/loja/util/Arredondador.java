package com.loja.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
  private static final int SCALE = 2;

  public static BigDecimal arredondar(BigDecimal valor) {
    if (valor == null) {
      return BigDecimal.ZERO;
    }
    return valor.setScale(SCALE, RoundingMode.HALF_EVEN);
  }

  public static BigDecimal arredondar(double valor) {
    return arredondar(BigDecimal.valueOf(valor));
  }
}
