package pousada.plan;

import pousada.Money;
import pousada.Stay;

import java.math.BigDecimal;

public class NonRefundablePlan extends DiscountedPlan {
    public String code() { return "NON_REFUNDABLE"; }

    protected String discountFraction(Stay stay) { return "0.15"; }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
