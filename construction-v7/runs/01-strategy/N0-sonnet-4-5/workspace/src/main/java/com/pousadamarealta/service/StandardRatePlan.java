package com.pousadamarealta.service;

import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlanStrategy {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate date = checkIn;

        for (int i = 0; i < nights; i++) {
            BigDecimal nightRate = calculateNightRate(room.getType(), date);
            subtotal = subtotal.add(nightRate);
            date = date.plusDays(1);
        }

        subtotal = round(subtotal);
        return new PricingResult(subtotal, BigDecimal.ZERO, subtotal);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
