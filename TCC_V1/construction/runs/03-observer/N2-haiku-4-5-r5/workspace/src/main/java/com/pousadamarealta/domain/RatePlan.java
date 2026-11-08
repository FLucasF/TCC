package com.pousadamarealta.domain;

public enum RatePlan {
  STANDARD("STANDARD", 0.0),
  NON_REFUNDABLE("NON_REFUNDABLE", 0.15);

  public final String value;
  public final double discountRate;

  RatePlan(String value, double discountRate) {
    this.value = value;
    this.discountRate = discountRate;
  }

  public static RatePlan from(String value) {
    for (RatePlan plan : RatePlan.values()) {
      if (plan.value.equals(value)) {
        return plan;
      }
    }
    throw new IllegalArgumentException("Unknown rate plan: " + value);
  }
}
