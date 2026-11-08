package com.marealta.plan;

import com.marealta.Money;
import java.math.BigDecimal;

class NonRefundablePlan extends PerNightPlan {
    @Override public String code() { return "NON_REFUNDABLE"; }

    @Override protected String discountRate(BookingTerms terms) { return "0.15"; }

    @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
