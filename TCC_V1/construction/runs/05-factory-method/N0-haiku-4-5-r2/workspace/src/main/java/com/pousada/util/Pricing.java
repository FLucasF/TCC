package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Pricing {
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static boolean isPeakSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12) return day >= 15;
        if (month == 1 || month == 2) return true;
        return false;
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isPeakSeason(date)) {
            return baseRate.multiply(PEAK_MULTIPLIER).setScale(2, ROUNDING);
        }
        return baseRate.setScale(2, ROUNDING);
    }

    public static BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights) {
        return nightlyRate.multiply(new BigDecimal(nights)).setScale(2, ROUNDING);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return subtotal.multiply(DISCOUNT_RATE).setScale(2, ROUNDING);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, ROUNDING);
    }

    public static BigDecimal calculateIss(BigDecimal total) {
        return total.multiply(ISS_RATE).setScale(2, ROUNDING);
    }
}
