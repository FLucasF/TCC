package com.pousadamareaalta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.2");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }
        if (month == Month.DECEMBER) {
            return dayOfMonth >= 15;
        }
        if (month == Month.JANUARY) {
            return true;
        }
        if (month == Month.FEBRUARY) {
            return dayOfMonth <= 28;
        }
        return false;
    }

    public static double calculateNightlyPrice(double nightlyRate, LocalDate date) {
        BigDecimal rate = new BigDecimal(String.format("%.2f", nightlyRate));
        BigDecimal price = isHighSeason(date) ? rate.multiply(HIGH_SEASON_MULTIPLIER) : rate;
        return price.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static double calculateSubtotal(double nightlyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            double nightPrice = calculateNightlyPrice(nightlyRate, current);
            subtotal = subtotal.add(new BigDecimal(String.format("%.2f", nightPrice)));
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static double calculateDiscount(double subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            BigDecimal sub = new BigDecimal(String.format("%.2f", subtotal));
            BigDecimal discount = sub.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
            return discount.doubleValue();
        }
        return 0.0;
    }

    public static double calculateTotal(double subtotal, double discount) {
        BigDecimal sub = new BigDecimal(String.format("%.2f", subtotal));
        BigDecimal disc = new BigDecimal(String.format("%.2f", discount));
        return sub.subtract(disc).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }
}
