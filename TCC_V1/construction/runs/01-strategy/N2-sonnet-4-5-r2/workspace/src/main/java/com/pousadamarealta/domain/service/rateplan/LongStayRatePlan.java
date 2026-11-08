package com.pousadamarealta.domain.service.rateplan;

import com.pousadamarealta.domain.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.pousadamarealta.domain.service.rateplan.SeasonCalculator.*;

public class LongStayRatePlan implements RatePlan {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < MINIMUM_NIGHTS) {
            throw new IllegalArgumentException("Estadia longa requer no mínimo 7 noites");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = applyHighSeasonIfNeeded(roomType.getNightlyRate(), current);
            total = total.add(nightRate);
            current = current.plusDays(1);
        }

        return roundHalfToEven(total);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return roundHalfToEven(subtotal.multiply(DISCOUNT_RATE));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
