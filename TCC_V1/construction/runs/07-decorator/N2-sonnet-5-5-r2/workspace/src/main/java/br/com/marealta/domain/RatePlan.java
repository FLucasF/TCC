package br.com.marealta.domain;

import java.math.BigDecimal;

/** Plano de tarifa: cada plano sabe seu desconto e quanto devolve no cancelamento. */
public enum RatePlan {
    STANDARD {
        @Override public BigDecimal discount(BigDecimal subtotal) { return Money.ZERO; }
        @Override public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
    },
    NON_REFUNDABLE {
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

        @Override public BigDecimal discount(BigDecimal subtotal) {
            return Money.round(subtotal.multiply(DISCOUNT_RATE));
        }
        @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
    };

    public abstract BigDecimal discount(BigDecimal subtotal);

    public abstract BigDecimal refund(BigDecimal amountPaid);
}
