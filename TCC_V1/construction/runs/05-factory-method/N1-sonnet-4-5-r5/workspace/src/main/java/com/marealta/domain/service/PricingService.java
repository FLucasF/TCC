package com.marealta.domain.service;

import com.marealta.domain.model.RatePlan;
import com.marealta.domain.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

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

    public PricingResult calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = calculateNights(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut, nights);
        BigDecimal discount = calculateDiscount(subtotal, ratePlan);
        BigDecimal total = subtotal.subtract(discount);

        return new PricingResult(nights, subtotal, discount, total);
    }

    private int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) checkIn.until(checkOut).getDays();
    }

    private BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightRate = room.nightlyRate();
            if (isHighSeason(currentDate)) {
                nightRate = nightRate.multiply(HIGH_SEASON_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_EVEN);
            }
            subtotal = subtotal.add(nightRate);
            currentDate = currentDate.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        return false;
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return subtotal.multiply(NON_REFUNDABLE_DISCOUNT)
                .setScale(2, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }
}
