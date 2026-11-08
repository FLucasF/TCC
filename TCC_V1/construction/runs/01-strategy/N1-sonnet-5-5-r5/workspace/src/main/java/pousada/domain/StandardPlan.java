package pousada.domain;

import java.math.BigDecimal;

class StandardPlan implements RatePlan {
    @Override
    public String code() {
        return "STANDARD";
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
