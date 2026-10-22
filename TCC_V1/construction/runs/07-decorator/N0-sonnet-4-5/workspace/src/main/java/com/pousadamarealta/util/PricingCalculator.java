package com.pousadamarealta.util;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

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

    public static PricingResult calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightlyRate = room.getNightlyRate();

            if (isHighSeason(currentDate)) {
                nightlyRate = nightlyRate.multiply(HIGH_SEASON_MULTIPLIER);
                nightlyRate = round(nightlyRate);
            }

            subtotal = subtotal.add(nightlyRate);
            currentDate = currentDate.plusDays(1);
        }

        subtotal = round(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT);
            discount = round(discount);
        }

        BigDecimal total = subtotal.subtract(discount);
        total = round(total);

        return new PricingResult(nights, subtotal, discount, total);
    }

    private static boolean isHighSeason(LocalDate date) {
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

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
