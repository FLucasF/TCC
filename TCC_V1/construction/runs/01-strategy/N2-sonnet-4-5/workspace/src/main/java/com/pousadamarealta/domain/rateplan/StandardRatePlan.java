package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlan {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        return PriceCalculator.calculateRegularSubtotal(room, checkIn, checkOut);
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
