package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

public class NonRefundablePlan extends NightlyPlan {
    @Override public String code() { return "NON_REFUNDABLE"; }
    @Override protected BigDecimal discountRate(Stay stay) { return new BigDecimal("0.15"); }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
