package br.com.pousadamareal.service;

import br.com.pousadamareal.domain.RatePlan;
import br.com.pousadamareal.util.DateUtils;
import br.com.pousadamareal.util.MoneyUtils;

import java.time.LocalDate;

public class PricingService {
    public static class PricingResult {
        public int nights;
        public double nightlyRate;
        public double subtotal;
        public double discount;
        public double total;

        public PricingResult(int nights, double nightlyRate, double subtotal, double discount, double total) {
            this.nights = nights;
            this.nightlyRate = nightlyRate;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    public PricingResult calculatePrice(LocalDate checkIn, LocalDate checkOut, double baseNightlyRate, RatePlan ratePlan) {
        int nights = DateUtils.calculateNights(checkIn, checkOut);

        double nightlyRate = calculateNightlyRate(checkIn, baseNightlyRate);
        double nightlyRateRounded = MoneyUtils.round(nightlyRate);

        double subtotal = 0;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            subtotal += nightlyRateRounded;
            current = current.plusDays(1);
        }
        subtotal = MoneyUtils.round(subtotal);

        double discountAmount = calculateDiscount(subtotal, ratePlan);
        discountAmount = MoneyUtils.round(discountAmount);

        double total = MoneyUtils.round(subtotal - discountAmount);

        return new PricingResult(nights, nightlyRateRounded, subtotal, discountAmount, total);
    }

    private double calculateNightlyRate(LocalDate checkIn, double baseRate) {
        if (DateUtils.isHighSeason(checkIn)) {
            return baseRate * 1.20;
        }
        return baseRate;
    }

    private double calculateDiscount(double subtotal, RatePlan ratePlan) {
        return subtotal * ratePlan.getDiscountRate();
    }
}
