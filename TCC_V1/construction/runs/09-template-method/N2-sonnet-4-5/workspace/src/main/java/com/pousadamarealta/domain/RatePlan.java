package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum RatePlan {
    STANDARD {
        @Override
        public BigDecimal applyDiscount(BigDecimal subtotal) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },
    NON_REFUNDABLE {
        @Override
        public BigDecimal applyDiscount(BigDecimal subtotal) {
            return subtotal.multiply(new BigDecimal("0.15"))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
    };

    public abstract BigDecimal applyDiscount(BigDecimal subtotal);
    public abstract BigDecimal calculateRefund(BigDecimal amountPaid);
}
