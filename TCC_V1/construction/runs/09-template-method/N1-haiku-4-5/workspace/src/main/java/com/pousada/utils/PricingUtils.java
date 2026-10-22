package com.pousada.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingUtils {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 || month == 1 || month == 2) {
            if (month == 12) {
                return day >= 15;
            }
            if (month == 2) {
                return day <= 28;
            }
            return true;
        }

        return false;
    }

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate night) {
        BigDecimal price = baseRate;
        if (isHighSeason(night)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return roundToPair(price);
    }

    public static BigDecimal calculateSubtotal(BigDecimal[] nightlyPrices) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : nightlyPrices) {
            sum = sum.add(price);
        }
        return roundToPair(sum);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
        return roundToPair(discount);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundToPair(subtotal.subtract(discount));
    }

    public static BigDecimal roundToPair(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return "R$ " + String.format("%.2f", value).replace(".", ",");
    }

    public static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public static String formatDateIso(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
