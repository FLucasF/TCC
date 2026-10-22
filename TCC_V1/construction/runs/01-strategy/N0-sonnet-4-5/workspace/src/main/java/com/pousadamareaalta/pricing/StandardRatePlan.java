package com.pousadamareaalta.pricing;

import com.pousadamareaalta.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlanStrategy {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
    }

    @Override
    public PricingResult calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(roomType, checkIn, nights);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }

    protected BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate currentDate = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightPrice = SeasonUtil.calculateNightPrice(roomType.getNightlyRate(), currentDate);
            subtotal = subtotal.add(nightPrice);
            currentDate = currentDate.plusDays(1);
        }

        return SeasonUtil.round(subtotal);
    }
}
