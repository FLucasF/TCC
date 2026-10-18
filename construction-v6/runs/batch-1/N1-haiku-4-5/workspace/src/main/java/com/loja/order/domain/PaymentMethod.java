package com.loja.order.domain;

public enum PaymentMethod {
  PIX,
  CREDIT_CARD,
  BANK_SLIP;

  public boolean supportsInstallments() {
    return this == CREDIT_CARD;
  }

  public boolean supportsMultipleInstallments() {
    return this == CREDIT_CARD;
  }

  public int getMaxInstallments() {
    return this == CREDIT_CARD ? 12 : 1;
  }
}
