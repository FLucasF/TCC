package com.pousada.marealta.service;

import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.util.MoneyRounder;
import com.pousada.marealta.util.SeasonCalculator;

import java.time.LocalDate;

public class PriceCalculator {
    public static class PriceBreakdown {
        public final int nights;
        public final double subtotal;
        public final double discount;
        public final double total;

        public PriceBreakdown(int nights, double subtotal, double discount, double total) {
            this.nights = nights;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public static PriceBreakdown calculate(double baseNightlyRate, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        double subtotal = 0.0;
        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            double nightlyRate = SeasonCalculator.getNightlyRate(baseNightlyRate, nightDate);
            nightlyRate = MoneyRounder.round(nightlyRate);
            subtotal += nightlyRate;
        }
        subtotal = MoneyRounder.round(subtotal);

        double discount = 0.0;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = subtotal * ratePlan.getDiscountRate();
            discount = MoneyRounder.round(discount);
        }

        double total = subtotal - discount;
        total = MoneyRounder.round(total);

        return new PriceBreakdown(nights, subtotal, discount, total);
    }
}
