package com.pousadamarealta.util;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {

    public static BigDecimal calculateNightlyPrice(Room room, LocalDate date) {
        BigDecimal baseRate = room.getNightlyRate();
        if (isHighSeason(date)) {
            BigDecimal multiplier = new BigDecimal("1.20");
            return round(baseRate.multiply(multiplier));
        }
        return baseRate;
    }

    public static BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightlyPrice = calculateNightlyPrice(room, current);
            subtotal = subtotal.add(nightlyPrice);
            current = current.plusDays(1);
        }

        return round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            BigDecimal discountRate = new BigDecimal("0.15");
            return round(subtotal.multiply(discountRate));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateIss(BigDecimal total) {
        BigDecimal issRate = new BigDecimal("0.05");
        return round(total.multiply(issRate));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == 7) {
            return true;
        }

        // 15/12 até fim de fevereiro
        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
