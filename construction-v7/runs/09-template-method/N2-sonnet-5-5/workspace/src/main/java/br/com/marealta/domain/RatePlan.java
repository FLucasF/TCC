package br.com.marealta.domain;

import java.math.BigDecimal;

/** Cada plano sabe seu desconto e sua política de reembolso. */
public enum RatePlan {
    STANDARD {
        @Override public BigDecimal discount(BigDecimal subtotal) { return Money.ZERO; }
        @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.round(amountPaid); }
    },
    NON_REFUNDABLE {
        @Override public BigDecimal discount(BigDecimal subtotal) {
            return Money.round(subtotal.multiply(new BigDecimal("0.15")));
        }
        @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
    };

    public abstract BigDecimal discount(BigDecimal subtotal);

    public abstract BigDecimal refund(BigDecimal amountPaid);
}
