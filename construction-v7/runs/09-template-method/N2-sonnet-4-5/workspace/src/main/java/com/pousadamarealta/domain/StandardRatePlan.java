package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StandardRatePlan implements RatePlan {
    @Override
    public RatePlanType getType() {
        return RatePlanType.STANDARD;
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
