package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.pousada.marealta.model.RatePlan;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public static BigDecimal roundPrice(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (SeasonHelper.isHighSeason(date)) {
            return roundPrice(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(baseRate, current);
            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }

        return roundPrice(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return roundPrice(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundPrice(subtotal.subtract(discount));
    }
}
