package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = BigDecimal.valueOf(0.15);

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 1) return true;
        if (month == 2) return true;
        if (month == 12 && day >= 15) return true;

        return false;
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate;
    }

    public static BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights) {
        return nightlyRate.multiply(BigDecimal.valueOf(nights)).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static class PriceBreakdown {
        public int nights;
        public BigDecimal nightlyRate;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PriceBreakdown(int nights, BigDecimal nightlyRate, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.nightlyRate = nightlyRate;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PriceBreakdown calculatePrice(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut, String ratePlan) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(baseRate, currentDate);
            subtotal = subtotal.add(nightlyRate).setScale(2, java.math.RoundingMode.HALF_EVEN);
            currentDate = currentDate.plusDays(1);
        }

        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = calculateTotal(subtotal, discount);

        BigDecimal nightlyRate = calculateNightlyRate(baseRate, checkIn);

        return new PriceBreakdown(nights, nightlyRate, subtotal, discount, total);
    }
}
