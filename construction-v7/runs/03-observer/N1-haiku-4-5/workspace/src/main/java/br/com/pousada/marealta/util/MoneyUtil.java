package br.com.pousada.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtil {
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.15");

    public static double round(double value) {
        return new BigDecimal(value)
                .setScale(2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }

    public static double calculateNightlyRate(double baseRate, boolean isHighSeason) {
        double rate = baseRate;
        if (isHighSeason) {
            rate = rate * 1.20;
        }
        return round(rate);
    }

    public static double calculateSubtotal(double nightlyRate, int nights) {
        double subtotal = nightlyRate * nights;
        return round(subtotal);
    }

    public static double calculateDiscount(double subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return 0;
        }
        BigDecimal discount = new BigDecimal(subtotal).multiply(DISCOUNT_PERCENTAGE);
        return round(discount.doubleValue());
    }

    public static double calculateTotal(double subtotal, double discount) {
        double total = subtotal - discount;
        return round(total);
    }
}
