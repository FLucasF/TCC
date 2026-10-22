package com.pousada.marealta.util;

import com.pousada.marealta.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = BigDecimal.valueOf(0.15);

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return Money.round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public static BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights) {
        return Money.round(nightlyRate.multiply(BigDecimal.valueOf(nights)));
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, Reservation.RatePlan ratePlan) {
        if (ratePlan == Reservation.RatePlan.NON_REFUNDABLE) {
            return Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return Money.round(subtotal.subtract(discount));
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12) {
            return true;
        }

        if (month >= 1 && month <= 2) {
            return true;
        }

        return false;
    }
}
