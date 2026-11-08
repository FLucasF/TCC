package com.pousada.rateplan;

import com.pousada.util.Dates;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {
    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(int roomCapacity, int nights, LocalDate checkIn, LocalDate checkOut,
                         int guests, Map<String, Object> context) throws RatePlanException {
        String companyCode = (String) context.get("companyCode");
        if (companyCode == null || !DISCOUNTS.containsKey(companyCode)) {
            throw new RatePlanException("Invalid or missing company code for CORPORATE plan");
        }
    }

    @Override
    public PriceCalculation calculate(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut,
                                      Map<String, Object> context) {
        String companyCode = (String) context.get("companyCode");
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate date = checkIn.plusDays(i);
            BigDecimal rate = nightlyRate;
            if (Dates.isHighSeason(date)) {
                rate = rate.multiply(BigDecimal.valueOf(1.20));
            }
            rate = Money.round(rate);
            subtotal = subtotal.add(rate);
        }
        subtotal = Money.round(subtotal);

        BigDecimal discountPercentage = DISCOUNTS.get(companyCode);
        BigDecimal discount = subtotal.multiply(discountPercentage);
        discount = Money.round(discount);

        BigDecimal total = subtotal.subtract(discount);
        total = Money.round(total);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
