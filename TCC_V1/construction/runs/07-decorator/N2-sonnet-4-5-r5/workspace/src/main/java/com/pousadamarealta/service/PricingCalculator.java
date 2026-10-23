package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static class PricingResult {
        private final int nights;
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal total;

        public PricingResult(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }

        public int getNights() {
            return nights;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }

    public PricingResult calculate(RoomType roomType, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = (int) checkIn.until(checkOut).getDays();

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightRate = calculateNightRate(roomType, currentDate);
            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        subtotal = round(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }

        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(nights, subtotal, discount, total);
    }

    private BigDecimal calculateNightRate(RoomType roomType, LocalDate date) {
        BigDecimal baseRate = roomType.getNightlyRate();
        if (isHighSeason(date)) {
            return round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.JULY) {
            return true;
        }

        return false;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
