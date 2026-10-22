package br.com.marealta.domain;

import java.math.BigDecimal;

public enum RatePlan {
    STANDARD {
        @Override
        public BigDecimal discount(BigDecimal subtotal) {
            return Money.round(BigDecimal.ZERO);
        }

        @Override
        public BigDecimal refund(BigDecimal paid) {
            return paid;
        }
    },
    NON_REFUNDABLE {
        @Override
        public BigDecimal discount(BigDecimal subtotal) {
            return Money.round(subtotal.multiply(new BigDecimal("0.15")));
        }

        @Override
        public BigDecimal refund(BigDecimal paid) {
            return Money.round(BigDecimal.ZERO);
        }
    };

    public abstract BigDecimal discount(BigDecimal subtotal);

    public abstract BigDecimal refund(BigDecimal paid);
}
