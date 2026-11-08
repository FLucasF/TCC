package com.pousada.rateplan;

import com.pousada.util.Dates;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class StandardRatePlan implements RatePlan {
    @Override
    public void validate(int roomCapacity, int nights, LocalDate checkIn, LocalDate checkOut,
                         int guests, Map<String, Object> context) throws RatePlanException {
        // Standard plan has no special restrictions
    }

    @Override
    public PriceCalculation calculate(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut,
                                      Map<String, Object> context) {
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

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
