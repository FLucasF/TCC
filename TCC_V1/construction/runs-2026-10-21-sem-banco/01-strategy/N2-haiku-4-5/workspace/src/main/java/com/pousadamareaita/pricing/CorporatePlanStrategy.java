package com.pousadamareaita.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporatePlanStrategy implements RatePlanStrategy {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", BigDecimal.valueOf(0.12),
        "AGROVALE", BigDecimal.valueOf(0.18),
        "FARMAVIDA", BigDecimal.valueOf(0.25)
    );

    private String companyCode;

    public CorporatePlanStrategy(String companyCode) {
        this.companyCode = companyCode;
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int guests, int roomCapacity, String companyCode) {
        if (companyCode == null || companyCode.isEmpty() || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new IllegalArgumentException("Invalid company code");
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
        BigDecimal discountRate = COMPANY_DISCOUNTS.getOrDefault(companyCode, BigDecimal.ZERO);
        BigDecimal discount = PricingUtils.roundBanker(subtotal.multiply(discountRate));
        BigDecimal total = subtotal.subtract(discount);

        return new PriceCalculation(nights, subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
