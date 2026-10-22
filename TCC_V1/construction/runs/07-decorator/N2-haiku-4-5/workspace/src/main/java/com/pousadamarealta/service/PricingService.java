package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            price = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return roundBankersStyle(price);
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightlyPrice = calculateNightlyPrice(baseRate, current);
            nightlyPrice = roundBankersStyle(nightlyPrice);
            subtotal = subtotal.add(nightlyPrice);
            current = current.plusDays(1);
        }
        return subtotal;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return roundBankersStyle(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount);
    }

    public int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER) {
            return dayOfMonth >= 15;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public BigDecimal roundBankersStyle(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
