package com.pousada.rateplan;

import com.pousada.util.Dates;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class LongStayRatePlan implements RatePlan {
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.10");

    @Override
    public void validate(int roomCapacity, int nights, LocalDate checkIn, LocalDate checkOut,
                         int guests, Map<String, Object> context) throws RatePlanException {
        if (nights < 7) {
            throw new RatePlanException("Long stay plan requires at least 7 nights");
        }
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

        BigDecimal discount = subtotal.multiply(DISCOUNT_PERCENTAGE);
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
