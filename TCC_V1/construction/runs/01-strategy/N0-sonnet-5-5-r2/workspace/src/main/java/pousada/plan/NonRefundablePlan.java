package pousada.plan;

import pousada.Money;

import java.math.BigDecimal;

public class NonRefundablePlan implements RatePlan {
    private static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    public String code() { return "NON_REFUNDABLE"; }

    public Quote quote(BookingContext ctx) {
        return Quote.withDiscountRate(ctx.seasonalSubtotal(), DISCOUNT);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
