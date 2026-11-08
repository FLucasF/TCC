package br.com.marealta.plan;

import java.math.BigDecimal;

public final class StandardPlan implements RatePlan {
    @Override public String code() { return "STANDARD"; }

    @Override public Quote quote(PricingContext ctx) {
        return Quote.withoutDiscount(ctx.seasonalSubtotal());
    }

    @Override public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
