package br.com.marealta.plan;

import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

final class StandardPlan extends DiscountedNightsPlan {
    StandardPlan() {
        super("STANDARD");
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
