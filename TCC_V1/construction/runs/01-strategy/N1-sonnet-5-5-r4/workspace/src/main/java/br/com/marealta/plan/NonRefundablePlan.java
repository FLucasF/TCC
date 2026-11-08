package br.com.marealta.plan;

import br.com.marealta.Money;
import br.com.marealta.Stay;
import java.math.BigDecimal;

public class NonRefundablePlan extends RatePlan {
    private static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    public NonRefundablePlan() {
        super("NON_REFUNDABLE");
    }

    @Override
    protected BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.fraction(subtotal, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal paid) {
        return Money.ZERO;
    }
}
