package br.com.marealta.plan;

import java.math.BigDecimal;

public class NonRefundablePlan extends AbstractRatePlan {
    public String code() { return "NON_REFUNDABLE"; }
    public Quote quote(QuoteRequest r) { return withDiscount(r, "0.15"); }
    public BigDecimal refund(BigDecimal paid) { return Money.round(BigDecimal.ZERO); }
}
