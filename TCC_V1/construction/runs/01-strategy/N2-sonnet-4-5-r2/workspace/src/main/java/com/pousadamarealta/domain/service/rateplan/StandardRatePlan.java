package com.pousadamarealta.domain.service.rateplan;

import com.pousadamarealta.domain.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.pousadamarealta.domain.service.rateplan.SeasonCalculator.*;

public class StandardRatePlan implements RatePlan {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
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
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
