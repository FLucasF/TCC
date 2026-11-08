package com.pousadamarealta.services;

import com.pousadamarealta.models.RatePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = BigDecimal.valueOf(0.15);

    public boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return (month == Month.DECEMBER || month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY);
    }

    public BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyRate(baseRate, current));
            current = current.plusDays(1);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
