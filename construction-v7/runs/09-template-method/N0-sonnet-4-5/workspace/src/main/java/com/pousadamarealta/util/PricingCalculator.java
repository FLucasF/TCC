package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static BigDecimal roundToCents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate date) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return roundToCents(price);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(baseRate, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return roundToCents(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return BigDecimal.ZERO;
        }
        return roundToCents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundToCents(subtotal.subtract(discount));
    }
}
