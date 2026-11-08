package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

final class NonRefundablePlan extends DiscountedNightsPlan {
    NonRefundablePlan() {
        super("NON_REFUNDABLE");
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        return new BigDecimal("0.15");
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.ZERO;
    }
}
