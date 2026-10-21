package com.pousada.service;

import com.pousada.domain.RatePlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    public BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate.setScale(2, RoundingMode.HALF_EVEN);
    }

    public PricingResult calculatePrice(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut,
                                       RatePlan ratePlan) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        LocalDate currentDate = checkIn;
        while (currentDate.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(baseRate, currentDate);
            subtotal = subtotal.add(nightlyRate);
            currentDate = currentDate.plusDays(1);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = subtotal.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);

        return new PricingResult(nights, subtotal, discount, total);
    }

    private boolean isHighSeason(LocalDate date) {
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

    public static class PricingResult {
        public final int nights;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PricingResult(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
