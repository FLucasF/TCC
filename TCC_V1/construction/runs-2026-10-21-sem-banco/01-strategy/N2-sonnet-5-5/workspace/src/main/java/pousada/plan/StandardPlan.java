package pousada.plan;

import pousada.Stay;

import java.math.BigDecimal;

public class StandardPlan extends DiscountedPlan {
    public String code() { return "STANDARD"; }

    protected String discountFraction(Stay stay) { return "0"; }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
