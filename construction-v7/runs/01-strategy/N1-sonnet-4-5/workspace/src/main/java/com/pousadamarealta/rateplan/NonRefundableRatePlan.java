package com.pousadamarealta.rateplan;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan extends StandardRatePlan {
    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = MoneyUtils.round(subtotal.multiply(new BigDecimal("0.15")));
        BigDecimal total = MoneyUtils.round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
