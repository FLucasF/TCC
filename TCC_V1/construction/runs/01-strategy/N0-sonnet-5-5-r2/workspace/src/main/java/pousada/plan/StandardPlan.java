package pousada.plan;

import java.math.BigDecimal;

public class StandardPlan implements RatePlan {
    public String code() { return "STANDARD"; }

    public Quote quote(BookingContext ctx) {
        return new Quote(ctx.seasonalSubtotal(), BigDecimal.ZERO);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
