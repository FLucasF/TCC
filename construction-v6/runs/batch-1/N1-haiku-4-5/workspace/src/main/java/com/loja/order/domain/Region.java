package com.loja.order.domain;

import java.math.BigDecimal;

public enum Region {
  SOUTHEAST(0.01, null),
  SOUTH(0.01, null),
  MIDWEST(0.015, null),
  NORTH(0.025, new BigDecimal("30.00")),
  NORTHEAST(0.02, new BigDecimal("30.00"));

  private final double insuranceRate;
  private final BigDecimal insuranceLimit;

  Region(double insuranceRate, BigDecimal insuranceLimit) {
    this.insuranceRate = insuranceRate;
    this.insuranceLimit = insuranceLimit;
  }

  public double getInsuranceRate() {
    return insuranceRate;
  }

  public BigDecimal getInsuranceLimit() {
    return insuranceLimit;
  }

  public int getCarrierDaysAddition(ShippingMethod method) {
    if (method == ShippingMethod.ECONOMY || method == ShippingMethod.EXPRESS) {
      return switch (this) {
        case NORTH -> 3;
        case NORTHEAST -> 2;
        default -> 0;
      };
    }
    return 0;
  }
}
