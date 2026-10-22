package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // July (month 7)
        if (month == 7) {
            return true;
        }

        // December 15 to February 28 (months 12, 1, 2)
        if (month == 12 && day >= 15) {
            return true;
        }
        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        BigDecimal price = baseRate;
        if (isHighSeason(date)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return price.setScale(SCALE, ROUNDING_MODE);
    }

    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return checkOut.toEpochDay() - checkIn.toEpochDay();
    }

    public static PriceBreakdown calculatePrice(BigDecimal baseRate, LocalDate checkIn,
                                                LocalDate checkOut, String ratePlan) {
        long nights = calculateNights(checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;
        for (int i = 0; i < nights; i++) {
            subtotal = subtotal.add(calculateNightlyPrice(baseRate, currentDate));
            currentDate = currentDate.plusDays(1);
        }
        subtotal = subtotal.setScale(SCALE, ROUNDING_MODE);

        BigDecimal discount = BigDecimal.ZERO;
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE)
                .setScale(SCALE, ROUNDING_MODE);
        }

        BigDecimal total = subtotal.subtract(discount)
            .setScale(SCALE, ROUNDING_MODE);

        return new PriceBreakdown(nights, subtotal, discount, total);
    }

    public record PriceBreakdown(long nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}
}
