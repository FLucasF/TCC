package com.pousada.rateplan;

import com.pousada.exception.BusinessException;
import java.math.BigDecimal;

public class LongStayRatePlan extends BaseRatePlanStrategy {

    @Override
    public void validate(PricingContext context) {
        if (context.getNights() < 7) {
            throw new BusinessException("INVALID_REQUEST",
                "Long stay rate plan requires at least 7 nights", 400);
        }
    }

    @Override
    public PricingResult calculatePrice(PricingContext context) {
        BigDecimal subtotal = calculateNightlySubtotal(context);
        BigDecimal discount = round(subtotal.multiply(new BigDecimal("0.10")));
        BigDecimal total = round(subtotal.subtract(discount));

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
