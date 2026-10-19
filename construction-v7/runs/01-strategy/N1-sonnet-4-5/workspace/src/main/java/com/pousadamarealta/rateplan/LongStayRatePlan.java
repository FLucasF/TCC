package com.pousadamarealta.rateplan;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LongStayRatePlan extends StandardRatePlan {
    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        if (nights < 7) {
            throw new IllegalArgumentException("Long stay requires at least 7 nights");
        }
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = MoneyUtils.round(subtotal.multiply(new BigDecimal("0.10")));
        BigDecimal total = MoneyUtils.round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }
}
