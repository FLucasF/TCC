package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month >= 12 || month <= 2) {
            if (month == 2) return true;
            if (month == 12) return true;
            if (month == 1) return true;
            return false;
        }

        if (month == 7) return true;

        return false;
    }

    public BigDecimal calculateNightlyRate(BigDecimal baseNightly, LocalDate date) {
        BigDecimal rate = baseNightly;
        if (isHighSeason(date)) {
            rate = rate.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return round(rate);
    }

    public BigDecimal calculateSubtotal(BigDecimal baseNightly, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyRate(baseNightly, current));
            current = current.plusDays(1);
        }
        return subtotal;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return BigDecimal.ZERO;
        }
        return round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    public int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) checkIn.until(checkOut, java.time.temporal.ChronoUnit.DAYS);
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
