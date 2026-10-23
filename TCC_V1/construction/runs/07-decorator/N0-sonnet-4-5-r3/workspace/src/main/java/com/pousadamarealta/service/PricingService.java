package com.pousadamarealta.service;

import com.pousadamarealta.model.RatePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public BigDecimal calculateNightlyRate(double baseRate, LocalDate date) {
        BigDecimal rate = BigDecimal.valueOf(baseRate);

        if (isHighSeason(date)) {
            rate = rate.multiply(HIGH_SEASON_MULTIPLIER);
        }

        return rate.setScale(SCALE, ROUNDING_MODE);
    }

    public BigDecimal calculateSubtotal(double baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(baseRate, currentDate);
            subtotal = subtotal.add(nightlyRate);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(SCALE, ROUNDING_MODE);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        double discountRate = ratePlan.getDiscountRate();
        if (discountRate == 0.0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(discountRate));
        return discount.setScale(SCALE, ROUNDING_MODE);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(SCALE, ROUNDING_MODE);
    }
}
