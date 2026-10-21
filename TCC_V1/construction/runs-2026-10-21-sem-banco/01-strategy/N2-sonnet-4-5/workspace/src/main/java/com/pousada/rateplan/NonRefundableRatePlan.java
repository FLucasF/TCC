package com.pousada.rateplan;

import java.math.BigDecimal;

public class NonRefundableRatePlan extends BaseRatePlanStrategy {

    @Override
    public void validate(PricingContext context) {
    }

    @Override
    public PricingResult calculatePrice(PricingContext context) {
        BigDecimal subtotal = calculateNightlySubtotal(context);
        BigDecimal discount = round(subtotal.multiply(new BigDecimal("0.15")));
        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return BigDecimal.ZERO;
    }
}
