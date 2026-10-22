package com.pousada.marealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, ROUNDING_MODE);
        }
        return baseRate.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyPrice(baseRate, current));
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (isNonRefundable) {
            return subtotal.multiply(DISCOUNT_RATE).setScale(2, ROUNDING_MODE);
        }
        return BigDecimal.ZERO.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, ROUNDING_MODE);
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();

        if (month == 7) {
            return true;
        }

        if (month == 12 || month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
