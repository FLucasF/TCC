package com.pousamarealta.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceService {

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12) {
            return day >= 15;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            BigDecimal rate = baseRate.multiply(new BigDecimal("1.20"));
            return rate.setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = calculateNightlyRate(baseRate, current);
            subtotal = subtotal.add(nightRate);
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            BigDecimal discount = subtotal.multiply(new BigDecimal("0.15"));
            return discount.setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal total = subtotal.subtract(discount);
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static BigDecimal calculateISS(BigDecimal total) {
        BigDecimal iss = total.multiply(new BigDecimal("0.05"));
        return iss.setScale(2, RoundingMode.HALF_EVEN);
    }
}
