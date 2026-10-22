package com.pousada.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
  private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
  private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

  public static class PriceCalculation {
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;

    public PriceCalculation(BigDecimal subtotal, BigDecimal discount, int nights) {
      this.nights = nights;
      this.subtotal = subtotal;
      this.discount = discount;
      this.total = subtotal.subtract(discount);
    }
  }

  public static PriceCalculation calculate(
      LocalDate checkIn, LocalDate checkOut, BigDecimal nightlyRate, String ratePlan) {
    int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

    BigDecimal subtotal = BigDecimal.ZERO;
    LocalDate current = checkIn;

    while (current.isBefore(checkOut)) {
      BigDecimal nightPrice = nightlyRate;

      if (isHighSeason(current)) {
        nightPrice = nightPrice.multiply(HIGH_SEASON_MULTIPLIER);
      }

      nightPrice = roundHalfEven(nightPrice);
      subtotal = subtotal.add(nightPrice);

      current = current.plusDays(1);
    }

    subtotal = roundHalfEven(subtotal);

    BigDecimal discount = BigDecimal.ZERO;
    if ("NON_REFUNDABLE".equals(ratePlan)) {
      discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
      discount = roundHalfEven(discount);
    }

    BigDecimal total = subtotal.subtract(discount);

    return new PriceCalculation(subtotal, discount, nights);
  }

  private static boolean isHighSeason(LocalDate date) {
    Month month = date.getMonth();
    int dayOfMonth = date.getDayOfMonth();

    if (month == Month.JULY) {
      return true;
    }

    if (month == Month.DECEMBER) {
      return true;
    }

    if (month == Month.JANUARY || month == Month.FEBRUARY) {
      return true;
    }

    return false;
  }

  public static BigDecimal roundHalfEven(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_EVEN);
  }
}
