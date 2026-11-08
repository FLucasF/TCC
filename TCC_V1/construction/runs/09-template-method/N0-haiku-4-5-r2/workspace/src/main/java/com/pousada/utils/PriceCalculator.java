package com.pousada.utils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static class PriceResult {
        public BigDecimal nightlyRate;
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PriceResult(BigDecimal nightlyRate, BigDecimal subtotal,
                          BigDecimal discount, BigDecimal total) {
            this.nightlyRate = nightlyRate;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PriceResult calculate(BigDecimal baseRate, LocalDate checkIn,
                                        LocalDate checkOut, String ratePlan) {
        int nights = DateUtils.daysBetween(checkIn, checkOut);

        BigDecimal nightlyRate = baseRate;
        if (isHighSeasonForAny(checkIn, checkOut)) {
            nightlyRate = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
        }
        nightlyRate = MoneyRounder.round(nightlyRate);

        BigDecimal subtotal = nightlyRate.multiply(new BigDecimal(nights));
        subtotal = MoneyRounder.round(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
            discount = MoneyRounder.round(discount);
        }

        BigDecimal total = subtotal.subtract(discount);
        total = MoneyRounder.round(total);

        return new PriceResult(nightlyRate, subtotal, discount, total);
    }

    private static boolean isHighSeasonForAny(LocalDate checkIn, LocalDate checkOut) {
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            if (DateUtils.isHighSeason(current)) {
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }
}
