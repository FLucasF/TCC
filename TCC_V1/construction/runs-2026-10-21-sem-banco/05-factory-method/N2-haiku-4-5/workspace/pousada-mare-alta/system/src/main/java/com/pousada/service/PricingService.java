package com.pousada.service;

import com.pousada.domain.RatePlan;
import com.pousada.util.MoneyRounder;
import java.time.LocalDate;

public class PricingService {
    private static final double HIGH_SEASON_MULTIPLIER = 1.2;
    private static final double NON_REFUNDABLE_DISCOUNT_RATE = 0.15;

    public record PricingResult(int nights, double subtotal, double discount, double total) {
    }

    public PricingResult calculatePrice(double nightlyRate, LocalDate checkIn, LocalDate checkOut,
            RatePlan ratePlan) {
        int nights = (int) checkOut.toEpochDay() - (int) checkIn.toEpochDay();

        double subtotal = 0;
        LocalDate currentDate = checkIn;
        while (currentDate.isBefore(checkOut)) {
            double rate = nightlyRate;
            if (isHighSeason(currentDate)) {
                rate = MoneyRounder.round(rate * HIGH_SEASON_MULTIPLIER);
            }
            subtotal = MoneyRounder.round(subtotal + rate);
            currentDate = currentDate.plusDays(1);
        }

        double discount = 0;
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            discount = MoneyRounder.round(subtotal * NON_REFUNDABLE_DISCOUNT_RATE);
        }

        double total = MoneyRounder.round(subtotal - discount);

        return new PricingResult(nights, subtotal, discount, total);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }
        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
