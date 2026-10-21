package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_SURCHARGE).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate;
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            total = total.add(calculateNightlyRate(baseRate, current));
            current = current.plusDays(1);
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateIssRetention(BigDecimal total) {
        return total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month >= 12) {
            return true;
        }
        if (month <= 2) {
            return true;
        }
        if (month == 7) {
            return true;
        }
        return false;
    }
}
