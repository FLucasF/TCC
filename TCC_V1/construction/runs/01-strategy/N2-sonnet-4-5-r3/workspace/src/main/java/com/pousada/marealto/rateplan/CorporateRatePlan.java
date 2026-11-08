package com.pousada.marealto.rateplan;

import com.pousada.marealto.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlanStrategy {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new IllegalArgumentException("Invalid company code for corporate rate plan");
        }
    }

    @Override
    public PriceCalculation calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights) {
        throw new IllegalStateException("Cannot calculate price without company code");
    }

    public PriceCalculation calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightlyPrice = SeasonCalculator.calculateNightlyPrice(roomType.getNightlyRate(), current);
            subtotal = subtotal.add(nightlyPrice);
            current = current.plusDays(1);
        }

        subtotal = SeasonCalculator.round(subtotal);
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = SeasonCalculator.round(subtotal.multiply(discountRate));
        BigDecimal total = SeasonCalculator.round(subtotal.subtract(discount));

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
