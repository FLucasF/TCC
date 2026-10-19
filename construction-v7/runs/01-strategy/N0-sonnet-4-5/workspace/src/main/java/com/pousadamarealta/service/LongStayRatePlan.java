package com.pousadamarealta.service;

import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan implements RatePlanStrategy {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights < 7) {
            throw new InvalidRequestException("O plano de estadia longa exige no mínimo 7 noites");
        }
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
        BigDecimal discount = round(subtotal.multiply(new BigDecimal("0.10")));
        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
