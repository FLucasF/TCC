package com.pousadamarealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {

    public static double calculateNightlyPrice(double baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return round(baseRate * 1.20);
        }
        return baseRate;
    }

    private static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        // Julho
        if (month == Month.JULY) {
            return true;
        }

        // 15 de dezembro até fim de fevereiro
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static double calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        double subtotal = 0.0;

        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            double nightlyPrice = calculateNightlyPrice(roomType.nightlyRate, nightDate);
            subtotal += nightlyPrice;
        }

        return round(subtotal);
    }

    public static double calculateDiscount(RatePlan ratePlan, double subtotal) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return round(subtotal * 0.15);
        }
        return 0.0;
    }

    public static double calculateTotal(double subtotal, double discount) {
        return round(subtotal - discount);
    }

    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static int getNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
