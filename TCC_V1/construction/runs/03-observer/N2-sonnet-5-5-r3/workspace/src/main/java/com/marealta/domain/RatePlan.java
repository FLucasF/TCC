package com.marealta.domain;

import java.math.BigDecimal;

/** Cada plano de tarifa traz as suas próprias regras de desconto e de reembolso. */
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
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

        @Override
        public BigDecimal discount(BigDecimal subtotal) {
            return Money.of(subtotal.multiply(DISCOUNT_RATE));
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return Money.ZERO;
        }
    };

    public abstract BigDecimal discount(BigDecimal subtotal);

    public abstract BigDecimal refund(BigDecimal amountPaid);
}
