package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import java.math.BigDecimal;

/** Estadia longa: 7 noites ou mais, 10% de desconto sobre as diárias. */
public class LongStayPlan implements RatePlan {
    static final int MIN_NIGHTS = 7;

    public String code() { return "LONG_STAY"; }

    public Quote quote(BookingRequest r) {
        if (r.nights() < MIN_NIGHTS) {
            throw new PlanRuleException("O plano LONG_STAY exige no mínimo " + MIN_NIGHTS + " noites");
        }
        return NightlyPricing.withPercentDiscount(r, 10);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.round(amountPaid); }
}
