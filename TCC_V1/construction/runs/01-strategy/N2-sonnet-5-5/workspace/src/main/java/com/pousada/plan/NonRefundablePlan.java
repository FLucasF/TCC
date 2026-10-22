package com.pousada.plan;

import com.pousada.domain.Money;

import java.math.BigDecimal;

public class NonRefundablePlan implements RatePlan {

    @Override
    public String code() {
        return "NON_REFUNDABLE";
    }

    @Override
    public BigDecimal discount(PricingRequest request, BigDecimal subtotal) {
        return Money.percent(subtotal, "0.15");
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.ZERO;
    }
}
