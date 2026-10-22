package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class MoneyUtil {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public static BigDecimal roundHalfToEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        int year = date.getYear();

        // July
        if (month == 7) {
            return true;
        }

        // December 15 to February 28/29
        if (month == 12 && day >= 15) {
            return true;
        }
        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public static BigDecimal getNightlyPrice(BigDecimal baseRate, LocalDate date) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return roundHalfToEven(price);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentNight = checkIn;

        while (currentNight.isBefore(checkOut)) {
            BigDecimal nightlyPrice = getNightlyPrice(baseRate, currentNight);
            subtotal = subtotal.add(nightlyPrice);
            currentNight = currentNight.plusDays(1);
        }

        return roundHalfToEven(subtotal);
    }

    @Deprecated
    public static BigDecimal calculateSubtotal(BigDecimal nightlyPrice, int nights) {
        return roundHalfToEven(nightlyPrice.multiply(new BigDecimal(nights)));
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return roundHalfToEven(subtotal.multiply(DISCOUNT_RATE));
        }
        return new BigDecimal("0.00");
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundHalfToEven(subtotal.subtract(discount));
    }

    public static BigDecimal calculateISS(BigDecimal total) {
        return roundHalfToEven(total.multiply(ISS_RATE));
    }
}
