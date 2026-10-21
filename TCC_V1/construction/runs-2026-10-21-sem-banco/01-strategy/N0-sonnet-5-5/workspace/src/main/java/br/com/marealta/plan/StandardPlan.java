package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import java.math.BigDecimal;

public class StandardPlan implements RatePlan {
    public String code() { return "STANDARD"; }

    public Quote quote(BookingRequest r) { return NightlyPricing.withPercentDiscount(r, 0); }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.round(amountPaid); }
}
