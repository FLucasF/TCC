package com.pousada.service.pricing;

import java.math.BigDecimal;

public abstract class RefundPolicy {
    public abstract BigDecimal calculateRefund(BigDecimal amountPaid);

    public static class FullRefund extends RefundPolicy {
        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    }

    public static class NoRefund extends RefundPolicy {
        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return BigDecimal.ZERO;
        }
    }

    public static class HalfRefund extends RefundPolicy {
        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return com.pousada.util.PriceCalculator.roundPrice(
                amountPaid.divide(BigDecimal.TWO, java.math.RoundingMode.HALF_EVEN)
            );
        }
    }
}
