package com.loja.checkout.model;

import java.math.BigDecimal;

public enum Regiao {
  SUDESTE(new BigDecimal("0.12")),
  SUL(new BigDecimal("0.11")),
  CENTRO_OESTE(new BigDecimal("0.09")),
  NORTE(new BigDecimal("0.07")),
  NORDESTE(new BigDecimal("0.07"));

  private final BigDecimal impostoPercentual;

  Regiao(BigDecimal impostoPercentual) {
    this.impostoPercentual = impostoPercentual;
  }

  public BigDecimal getImpostoPercentual() {
    return impostoPercentual;
  }

  public static boolean isValid(String code) {
    try {
      Regiao.valueOf(code);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
