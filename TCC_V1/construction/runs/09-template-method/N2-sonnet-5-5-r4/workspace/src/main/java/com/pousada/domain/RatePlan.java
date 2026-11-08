package com.pousada.domain;

import java.math.BigDecimal;

/** Cada plano sabe o seu desconto e o que devolve no cancelamento. */
public enum RatePlan {
    STANDARD {
        @Override
        public BigDecimal discount(BigDecimal subtotal) {
            return Money.ZERO;
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },
    NON_REFUNDABLE {
        private static final BigDecimal DISCOUNT = new BigDecimal("0.15");

        @Override
        public BigDecimal discount(BigDecimal subtotal) {
            return Money.round(subtotal.multiply(DISCOUNT));
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return Money.ZERO;
        }
    };

    public abstract BigDecimal discount(BigDecimal subtotal);

    public abstract BigDecimal refund(BigDecimal amountPaid);
}
