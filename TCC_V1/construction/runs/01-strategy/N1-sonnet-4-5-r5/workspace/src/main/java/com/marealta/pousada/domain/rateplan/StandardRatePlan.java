package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.domain.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan extends AbstractRatePlan {

    @Override
    public String getName() {
        return "STANDARD";
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        return calculateStandardSubtotal(checkIn, checkOut, roomType);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
