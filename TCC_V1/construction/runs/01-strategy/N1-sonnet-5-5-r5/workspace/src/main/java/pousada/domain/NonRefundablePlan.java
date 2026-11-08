package pousada.domain;

import java.math.BigDecimal;

class NonRefundablePlan implements RatePlan {
    private static final int DISCOUNT_PERCENT = 15;

    @Override
    public String code() {
        return "NON_REFUNDABLE";
    }

    @Override
    public BigDecimal discount(Booking booking, BigDecimal subtotal) {
        return Money.percent(subtotal, DISCOUNT_PERCENT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.ZERO;
    }
}
