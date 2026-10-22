package pousada.domain;

import java.math.BigDecimal;

/** Plano de tarifa: cada plano sabe seu desconto e quanto devolve no cancelamento. */
public enum RatePlan {
    STANDARD {
        @Override
        public BigDecimal discount(BigDecimal subtotal) {
            return Money.round(BigDecimal.ZERO);
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
            return Money.round(BigDecimal.ZERO);
        }
    };

    public abstract BigDecimal discount(BigDecimal subtotal);

    public abstract BigDecimal refund(BigDecimal amountPaid);
}
