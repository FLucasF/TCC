package com.pousada.marealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingCalculator {
  private static final BigDecimal PEAK_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

  public static boolean isPeakSeason(LocalDate date) {
    Month month = date.getMonth();
    int dayOfMonth = date.getDayOfMonth();

    if (month == Month.JULY) return true;
    if (month == Month.DECEMBER) return true;
    if (month == Month.JANUARY) return true;
    if (month == Month.FEBRUARY) return true;

    return false;
  }

  public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
    if (isPeakSeason(date)) {
      return baseRate.multiply(PEAK_SEASON_MULTIPLIER).setScale(2, ROUNDING_MODE);
    }
    return baseRate.setScale(2, ROUNDING_MODE);
  }

  public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
    BigDecimal subtotal = BigDecimal.ZERO;
    LocalDate currentDate = checkIn;

    while (currentDate.isBefore(checkOut)) {
      BigDecimal nightlyRate = calculateNightlyRate(baseRate, currentDate);
      subtotal = subtotal.add(nightlyRate);
      currentDate = currentDate.plusDays(1);
    }

    return subtotal.setScale(2, ROUNDING_MODE);
  }

  public static BigDecimal calculateDiscount(BigDecimal subtotal, double discountRate) {
    if (discountRate == 0.0) {
      return BigDecimal.ZERO.setScale(2, ROUNDING_MODE);
    }
    return subtotal.multiply(BigDecimal.valueOf(discountRate)).setScale(2, ROUNDING_MODE);
  }

  public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
    return subtotal.subtract(discount).setScale(2, ROUNDING_MODE);
  }

  public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
    return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
  }
}
