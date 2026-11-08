package br.com.marealta.plan;

import br.com.marealta.Money;

import java.math.BigDecimal;

public final class NonRefundablePlan implements RatePlan {
    private static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override public String code() { return "NON_REFUNDABLE"; }

    @Override public Quote quote(PricingContext ctx) {
        BigDecimal subtotal = ctx.seasonalSubtotal();
        return new Quote(subtotal, Money.times(subtotal, DISCOUNT));
    }

    @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
