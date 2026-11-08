package com.pousadamarealta.infrastructure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
  private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.2);

  public static boolean isHighSeason(LocalDate date) {
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();

    if (month == 7) return true;
    if (month == 12) return day >= 15;
    if (month == 1 || month == 2) return true;

    return false;
  }

  public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
    BigDecimal price = baseRate;
    if (isHighSeason(date)) {
      price = price.multiply(HIGH_SEASON_MULTIPLIER);
    }
    return round(price);
  }

  public static BigDecimal calculateSubtotal(BigDecimal nightlyPrice, int nights) {
    BigDecimal subtotal = nightlyPrice.multiply(BigDecimal.valueOf(nights));
    return round(subtotal);
  }

  public static BigDecimal calculateDiscount(BigDecimal subtotal, double discountRate) {
    if (discountRate == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(discountRate));
    return round(discount);
  }

  public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
    BigDecimal total = subtotal.subtract(discount);
    return round(total);
  }

  public static BigDecimal round(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_EVEN);
  }
}
