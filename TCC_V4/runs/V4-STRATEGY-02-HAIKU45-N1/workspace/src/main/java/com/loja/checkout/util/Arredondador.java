package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
  private static final int CASAS_DECIMAIS = 2;

  public static Double arredondar(double valor) {
    BigDecimal bd = new BigDecimal(Double.toString(valor));
    bd = bd.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
    return bd.doubleValue();
  }
}
