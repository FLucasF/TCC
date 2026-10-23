package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public final class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.JULY) {
            return true;
        }

        return false;
    }

    public static BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate night) {
        BigDecimal price = baseRate;
        if (isHighSeason(night)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return price.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(baseRate, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return discount.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, ROUNDING_MODE);
    }
}
