package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal PEAK_SEASON_INCREASE = new BigDecimal("0.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public static boolean isPeakSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month >= 12 && day >= 15) return true;
        if (month <= 2) return true;
        if (month == 7) return true;

        return false;
    }

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        if (isPeakSeason(date)) {
            BigDecimal increase = baseRate.multiply(PEAK_SEASON_INCREASE);
            return round(baseRate.add(increase));
        }
        return baseRate;
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        int nights = calculateNights(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightlyPrice = calculateNightlyPrice(baseRate, current);
            subtotal = subtotal.add(nightlyPrice);
            current = current.plusDays(1);
        }

        return round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            BigDecimal discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT);
            return round(discount);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
