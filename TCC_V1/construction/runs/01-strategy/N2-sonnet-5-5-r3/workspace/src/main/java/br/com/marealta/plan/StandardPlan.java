package br.com.marealta.plan;

import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

public class StandardPlan extends NightlyPlan {
    @Override public String code() { return "STANDARD"; }
    @Override protected BigDecimal discountRate(Stay stay) { return BigDecimal.ZERO; }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
