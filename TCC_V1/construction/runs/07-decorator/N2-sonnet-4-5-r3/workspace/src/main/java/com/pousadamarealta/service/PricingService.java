package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public final class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate night) {
        if (isHighSeason(night)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
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

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == Month.JULY) {
            return true;
        }

        // Dezembro a partir do dia 15
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        // Janeiro e fevereiro inteiros
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }
}
