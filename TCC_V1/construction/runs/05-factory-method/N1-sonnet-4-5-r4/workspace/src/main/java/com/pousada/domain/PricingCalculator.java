package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }
        return month == Month.JULY;
    }

    public static BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate night) {
        BigDecimal price = baseRate;
        if (isHighSeason(night)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return round(price);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightPrice(baseRate, current));
            current = current.plusDays(1);
        }
        return round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return round(discount);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
