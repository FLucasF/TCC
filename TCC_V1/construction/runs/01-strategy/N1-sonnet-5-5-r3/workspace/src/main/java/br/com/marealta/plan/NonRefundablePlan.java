package br.com.marealta.plan;

import java.math.BigDecimal;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Stay;

public class NonRefundablePlan implements RatePlan {
    @Override
    public String code() {
        return "NON_REFUNDABLE";
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.percent(subtotal, "0.15");
    }

    @Override
    public BigDecimal refund(BigDecimal paid) {
        return Money.ZERO;
    }
}
