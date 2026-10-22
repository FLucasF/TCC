package br.com.marealta.plan;

import java.math.BigDecimal;

public class StandardPlan extends AbstractRatePlan {
    public String code() { return "STANDARD"; }
    public Quote quote(QuoteRequest r) { return withDiscount(r, "0"); }
    public BigDecimal refund(BigDecimal paid) { return full(paid); }
}
