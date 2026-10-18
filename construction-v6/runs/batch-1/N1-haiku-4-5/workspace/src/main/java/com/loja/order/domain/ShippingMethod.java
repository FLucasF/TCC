package com.loja.order.domain;

public enum ShippingMethod {
  ECONOMY,
  EXPRESS,
  STORE_PICKUP,
  COURIER;

  public boolean usesCarrier() {
    return this == ECONOMY || this == EXPRESS;
  }

  public int getMaxWeightKg() {
    return this == COURIER ? 5 : Integer.MAX_VALUE;
  }
}
