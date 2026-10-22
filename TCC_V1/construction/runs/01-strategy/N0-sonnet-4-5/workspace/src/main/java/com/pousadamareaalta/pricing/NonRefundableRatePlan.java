package com.pousadamareaalta.pricing;

import com.pousadamareaalta.model.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan extends StandardRatePlan {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public PricingResult calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(roomType, checkIn, nights);
        BigDecimal discount = SeasonUtil.round(subtotal.multiply(DISCOUNT_RATE));
        BigDecimal total = SeasonUtil.round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
