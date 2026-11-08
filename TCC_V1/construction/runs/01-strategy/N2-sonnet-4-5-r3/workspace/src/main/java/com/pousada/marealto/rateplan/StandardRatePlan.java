package com.pousada.marealto.rateplan;

import com.pousada.marealto.domain.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlanStrategy {
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
        return new PriceCalculation(subtotal, BigDecimal.ZERO, subtotal);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
