package com.pousada.util;

import com.pousada.domain.RatePlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) return true;
        if (month == Month.DECEMBER) return true;
        if (month == Month.JANUARY) return true;
        if (month == Month.FEBRUARY && dayOfMonth <= 28) return true;

        return false;
    }

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER)
                    .setScale(SCALE, ROUNDING);
        }
        return baseRate.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal calculateSubtotal(BigDecimal nightlyPrice, int nights) {
        return nightlyPrice.multiply(new BigDecimal(nights))
                .setScale(SCALE, ROUNDING);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT)
                    .setScale(SCALE, ROUNDING);
        }
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount)
                .setScale(SCALE, ROUNDING);
    }

    public static BigDecimal calculateIss(BigDecimal total) {
        return total.multiply(new BigDecimal("0.05"))
                .setScale(SCALE, ROUNDING);
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
