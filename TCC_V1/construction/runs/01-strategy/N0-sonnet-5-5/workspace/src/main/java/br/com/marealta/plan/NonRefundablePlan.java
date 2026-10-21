package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import java.math.BigDecimal;

public class NonRefundablePlan implements RatePlan {
    public String code() { return "NON_REFUNDABLE"; }

    public Quote quote(BookingRequest r) { return NightlyPricing.withPercentDiscount(r, 15); }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
