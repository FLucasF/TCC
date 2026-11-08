package com.pousadamarealta.util;

import com.pousadamarealta.model.RatePlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = BigDecimal.valueOf(0.15);

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && dayOfMonth >= 15) return true;
        if (month == 1 || month == 2) return true;

        return false;
    }

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return roundHalfToEven(price, 2);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyPrice(baseRate, current));
            current = current.plusDays(1);
        }
        return roundHalfToEven(subtotal, 2);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return roundHalfToEven(subtotal.multiply(NON_REFUNDABLE_DISCOUNT), 2);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundHalfToEven(subtotal.subtract(discount), 2);
    }

    public static BigDecimal roundHalfToEven(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_EVEN);
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
