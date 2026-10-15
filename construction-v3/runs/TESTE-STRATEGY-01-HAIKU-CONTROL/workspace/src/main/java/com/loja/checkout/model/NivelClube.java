package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
  BRONZE(BigDecimal.ZERO),
  PRATA(new BigDecimal("0.02")),
  OURO(new BigDecimal("0.05"));

  private final BigDecimal creditoPercentual;

  NivelClube(BigDecimal creditoPercentual) {
    this.creditoPercentual = creditoPercentual;
  }

  public BigDecimal getCreditoPercentual() {
    return creditoPercentual;
  }

  public static boolean isValid(String code) {
    try {
      NivelClube.valueOf(code);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
