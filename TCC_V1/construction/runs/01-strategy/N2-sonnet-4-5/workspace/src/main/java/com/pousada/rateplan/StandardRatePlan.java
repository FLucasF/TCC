package com.pousada.rateplan;

import java.math.BigDecimal;

public class StandardRatePlan extends BaseRatePlanStrategy {

    @Override
    public void validate(PricingContext context) {
    }

    @Override
    public PricingResult calculatePrice(PricingContext context) {
        BigDecimal subtotal = calculateNightlySubtotal(context);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
