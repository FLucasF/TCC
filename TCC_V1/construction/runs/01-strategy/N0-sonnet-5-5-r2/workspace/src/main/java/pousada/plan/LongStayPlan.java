package pousada.plan;

import pousada.ApiException;

import java.math.BigDecimal;

/** 7 noites ou mais: 10% de desconto sobre as diárias. */
public class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    public String code() { return "LONG_STAY"; }

    @Override
    public void validate(BookingContext ctx) {
        if (ctx.nights() < MIN_NIGHTS) {
            throw ApiException.invalid("O LONG_STAY exige no mínimo " + MIN_NIGHTS + " noites");
        }
    }

    public Quote quote(BookingContext ctx) {
        return Quote.withDiscountRate(ctx.seasonalSubtotal(), DISCOUNT);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
