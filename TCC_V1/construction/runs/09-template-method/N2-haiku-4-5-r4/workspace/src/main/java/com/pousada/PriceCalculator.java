package com.pousada;

import java.time.LocalDate;

public class PriceCalculator {
    public static class PriceBreakdown {
        public int nights;
        public double subtotal;
        public double discount;
        public double total;

        public PriceBreakdown(int nights, double subtotal, double discount, double total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PriceBreakdown calculate(LocalDate checkIn, LocalDate checkOut, double nightlyRate, RatePlan ratePlan) {
        int nights = DateUtils.countNights(checkIn, checkOut);

        if (nights < 1) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        double subtotal = 0;
        LocalDate currentDate = checkIn;
        while (currentDate.isBefore(checkOut)) {
            double dailyRate = nightlyRate;
            if (DateUtils.isHighSeason(currentDate)) {
                dailyRate = Money.round(dailyRate * 1.2);
            }
            subtotal = Money.round(subtotal + dailyRate);
            currentDate = currentDate.plusDays(1);
        }

        double discount = 0;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = Money.round(subtotal * 0.15);
        }

        double total = Money.round(subtotal - discount);

        return new PriceBreakdown(nights, subtotal, discount, total);
    }
}
