package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // December 15 to end of February
        if ((month == 12 && day >= 15) || month == 1 || month == 2) {
            return true;
        }

        // Entire July
        return month == 7;
    }

    public static class PriceCalculation {
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PriceCalculation(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PriceCalculation calculatePrice(BigDecimal baseRate, LocalDate checkIn,
                                                   LocalDate checkOut, String ratePlan) {
        int nights = calculateNights(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate current = checkIn;
        for (int i = 0; i < nights; i++) {
            subtotal = subtotal.add(calculateNightlyRate(baseRate, current));
            current = current.plusDays(1);
        }

        BigDecimal discount = BigDecimal.ZERO;
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal total = subtotal.subtract(discount);

        return new PriceCalculation(subtotal, discount, total);
    }
}
