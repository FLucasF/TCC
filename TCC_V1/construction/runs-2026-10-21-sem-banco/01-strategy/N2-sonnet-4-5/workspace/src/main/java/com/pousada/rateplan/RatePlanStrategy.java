package com.pousada.rateplan;

import java.math.BigDecimal;

public interface RatePlanStrategy {
    void validate(PricingContext context);
    PricingResult calculatePrice(PricingContext context);
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
