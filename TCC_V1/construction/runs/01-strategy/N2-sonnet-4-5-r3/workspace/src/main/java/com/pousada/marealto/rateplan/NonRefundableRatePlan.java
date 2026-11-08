package com.pousada.marealto.rateplan;

import com.pousada.marealto.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan implements RatePlanStrategy {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
    }

    @Override
    public PriceCalculation calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightlyPrice = SeasonCalculator.calculateNightlyPrice(roomType.getNightlyRate(), current);
            subtotal = subtotal.add(nightlyPrice);
            current = current.plusDays(1);
        }

        subtotal = SeasonCalculator.round(subtotal);
        BigDecimal discount = SeasonCalculator.round(subtotal.multiply(DISCOUNT_RATE));
        BigDecimal total = SeasonCalculator.round(subtotal.subtract(discount));

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
