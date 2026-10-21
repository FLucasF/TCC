package com.pousadamareaita.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayPlanStrategy implements RatePlanStrategy {
    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int guests, int roomCapacity, String companyCode) {
        int nights = PricingUtils.countNights(checkIn, checkOut);
        if (nights < 7) {
            throw new IllegalArgumentException("Long stay requires at least 7 nights");
        }
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
        BigDecimal discount = PricingUtils.roundBanker(subtotal.multiply(BigDecimal.valueOf(0.10)));
        BigDecimal total = subtotal.subtract(discount);

        return new PriceCalculation(nights, subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
