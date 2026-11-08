package com.pousadamarealta.utils;

import com.pousadamarealta.domain.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        return (month == Month.DECEMBER && day >= 15) ||
               (month == Month.JANUARY) ||
               (month == Month.FEBRUARY && day <= 28) ||
               month == Month.JULY;
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return Formatting.round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
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
        return Formatting.round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return Formatting.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return Formatting.round(subtotal.subtract(discount));
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
