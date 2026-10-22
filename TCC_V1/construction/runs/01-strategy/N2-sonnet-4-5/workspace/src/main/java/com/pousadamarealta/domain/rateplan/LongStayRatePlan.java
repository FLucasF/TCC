package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.util.PriceCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LongStayRatePlan implements RatePlan {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 7) {
            throw new InvalidRequestException("Long stay requires minimum 7 nights");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        return PriceCalculator.calculateRegularSubtotal(room, checkIn, checkOut);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
