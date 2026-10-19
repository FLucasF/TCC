package com.pousada.strategy;

import java.math.BigDecimal;

public class StandardRatePlan implements RatePlanStrategy {
    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean isRefundable() {
        return true;
    }
}
