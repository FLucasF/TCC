package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.util.PriceCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class NonRefundableRatePlan implements RatePlan {

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        return PriceCalculator.calculateRegularSubtotal(room, checkIn, checkOut);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.15"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
