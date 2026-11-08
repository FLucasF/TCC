package br.com.marealta.pousada.domain;

import java.math.BigDecimal;

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

    /** Desconto sobre a soma das noites. */
    public abstract BigDecimal discount(BigDecimal subtotal);

    /** O que volta ao hóspede quando uma reserva paga é cancelada. */
    public abstract BigDecimal refund(BigDecimal amountPaid);
}
