package com.pousada.utils;

import com.pousada.models.RatePlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    public static BigDecimal calculateNightlyRate(LocalDate date, BigDecimal baseRate) {
        if (DateUtils.isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(SCALE, ROUNDING_MODE);
        }
        return baseRate.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal calculateSubtotal(
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal nightlyBaseRate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightRate = calculateNightlyRate(currentDate, nightlyBaseRate);
            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE).setScale(SCALE, ROUNDING_MODE);
        }
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(SCALE, ROUNDING_MODE);
    }
}
