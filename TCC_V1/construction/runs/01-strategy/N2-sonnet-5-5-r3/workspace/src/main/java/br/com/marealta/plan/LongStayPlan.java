package br.com.marealta.plan;

import br.com.marealta.DomainException;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

public class LongStayPlan extends NightlyPlan {
    private static final int MIN_NIGHTS = 7;

    @Override public String code() { return "LONG_STAY"; }

    @Override
    protected void validate(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw DomainException.invalid("O plano LONG_STAY exige pelo menos " + MIN_NIGHTS + " noites");
        }
    }

    @Override protected BigDecimal discountRate(Stay stay) { return new BigDecimal("0.10"); }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
