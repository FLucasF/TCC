package com.pousadamareaita.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardPlanStrategy implements RatePlanStrategy {
    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int guests, int roomCapacity, String companyCode) {
        if (guests < 1) {
            throw new IllegalArgumentException("Must have at least 1 guest");
        }
        if (guests > roomCapacity) {
            throw new IllegalArgumentException("Guests exceed room capacity");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    @Override
    public PriceCalculation calculatePrice(LocalDate checkIn, LocalDate checkOut, BigDecimal baseNightlyRate, int guests) {
        int nights = PricingUtils.countNights(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyRate = PricingUtils.getNightlyRate(baseNightlyRate, date);
            subtotal = subtotal.add(nightlyRate);
        }

        subtotal = PricingUtils.roundBanker(subtotal);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PriceCalculation(nights, subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
