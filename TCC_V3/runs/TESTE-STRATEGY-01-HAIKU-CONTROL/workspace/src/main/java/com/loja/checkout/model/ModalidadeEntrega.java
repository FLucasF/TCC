package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
  ECONOMICA(12.00, 2.00, 7),
  EXPRESSA(25.00, 4.50, 2),
  RETIRADA_LOJA(0.0, 0.0, 1),
  MOTOBOY(18.00, 0.0, 0);

  private final BigDecimal basePrice;
  private final BigDecimal pricePerKg;
  private final Integer prazo;

  ModalidadeEntrega(double basePrice, double pricePerKg, Integer prazo) {
    this.basePrice = BigDecimal.valueOf(basePrice);
    this.pricePerKg = BigDecimal.valueOf(pricePerKg);
    this.prazo = prazo;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public BigDecimal getPricePerKg() {
    return pricePerKg;
  }

  public Integer getPrazo() {
    return prazo;
  }

  public static boolean isValid(String code) {
    try {
      ModalidadeEntrega.valueOf(code);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
