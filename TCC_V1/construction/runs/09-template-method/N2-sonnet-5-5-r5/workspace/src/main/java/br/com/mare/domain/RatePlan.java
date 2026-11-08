package br.com.mare.domain;

import java.math.BigDecimal;

/** Plano de tarifa: cada plano sabe o seu desconto e o quanto devolve no cancelamento. */
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
        @Override
        public BigDecimal discount(BigDecimal subtotal) {
            return Money.round(subtotal.multiply(new BigDecimal("0.15")));
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return Money.ZERO;
        }
    };

    public abstract BigDecimal discount(BigDecimal subtotal);

    public abstract BigDecimal refund(BigDecimal amountPaid);
}
