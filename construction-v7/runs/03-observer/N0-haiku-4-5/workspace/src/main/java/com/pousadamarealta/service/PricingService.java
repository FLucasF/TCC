package com.pousadamarealta.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingService {
    private static final double HIGH_SEASON_MULTIPLIER = 1.20;
    private static final double NON_REFUNDABLE_DISCOUNT_RATE = 0.15;

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 || month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public static double roundHalfToEven(double value) {
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static double calculateNightlyPrice(double baseRate, LocalDate date) {
        double price = baseRate;
        if (isHighSeason(date)) {
            price *= HIGH_SEASON_MULTIPLIER;
        }
        return roundHalfToEven(price);
    }

    public static double calculateSubtotal(double[] nightlyPrices) {
        double sum = 0.0;
        for (double price : nightlyPrices) {
            sum += price;
        }
        return roundHalfToEven(sum);
    }

    public static double calculateDiscount(double subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return roundHalfToEven(subtotal * NON_REFUNDABLE_DISCOUNT_RATE);
        }
        return 0.0;
    }

    public static double calculateTotal(double subtotal, double discount) {
        return roundHalfToEven(subtotal - discount);
    }

    public static double[] calculateNightlyPrices(double baseRate, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        double[] prices = new double[nights];

        LocalDate current = checkIn;
        for (int i = 0; i < nights; i++) {
            prices[i] = calculateNightlyPrice(baseRate, current);
            current = current.plusDays(1);
        }

        return prices;
    }
}
