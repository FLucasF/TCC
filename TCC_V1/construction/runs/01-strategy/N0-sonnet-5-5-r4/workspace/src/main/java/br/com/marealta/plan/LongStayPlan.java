package br.com.marealta.plan;

import br.com.marealta.ApiException;
import br.com.marealta.Money;

import java.math.BigDecimal;

/** Estadia longa: 7 noites ou mais, 10% de desconto sobre as diárias. */
public final class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override public String code() { return "LONG_STAY"; }

    @Override public Quote quote(PricingContext ctx) {
        if (ctx.nights() < MIN_NIGHTS) {
            throw ApiException.invalid("O LONG_STAY exige " + MIN_NIGHTS + " noites ou mais.");
        }
        BigDecimal subtotal = ctx.seasonalSubtotal();
        return new Quote(subtotal, Money.times(subtotal, DISCOUNT));
    }

    @Override public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
