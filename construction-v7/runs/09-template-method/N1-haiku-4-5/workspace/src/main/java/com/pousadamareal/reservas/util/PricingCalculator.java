package com.pousadamareal.reservas.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");
    private static final int SCALE = 2;

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // July (7)
        if (month == 7) return true;

        // December 15 to February 28
        if (month == 12 && day >= 15) return true;
        if (month == 1) return true;
        if (month == 2) return true;

        return false;
    }

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            price = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return roundHalfEven(price);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightlyPrice(baseRate, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return roundHalfEven(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
        return roundHalfEven(discount);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundHalfEven(subtotal.subtract(discount));
    }

    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return checkOut.toEpochDay() - checkIn.toEpochDay();
    }

    public static BigDecimal roundHalfEven(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }
}
