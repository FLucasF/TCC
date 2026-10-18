package com.loja.order.domain;

public enum ClubTier {
  BRONZE,
  SILVER,
  GOLD;

  public double getCreditPercentage() {
    return switch (this) {
      case BRONZE -> 0.0;
      case SILVER -> 0.02;
      case GOLD -> 0.05;
    };
  }

  public boolean isFreeShippingAllowed() {
    return this == GOLD;
  }

  public double getShippingDiscount(ShippingMethod method) {
    if (this != GOLD || method == ShippingMethod.COURIER || method == ShippingMethod.STORE_PICKUP) {
      return 0;
    }
    return 1.0;
  }
}
