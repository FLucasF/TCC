package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightPrice(baseRate, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }

        return round(subtotal);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return round(subtotal.multiply(ratePlan.getDiscountRate()));
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        if (month == 7) {
            return true;
        }

        return false;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
