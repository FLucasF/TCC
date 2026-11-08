package pousada.domain;

import java.math.BigDecimal;

public class NonRefundablePlan extends NightlyRatePlan {
    @Override public String code() { return "NON_REFUNDABLE"; }
    @Override protected BigDecimal discountRate(Booking booking) { return Money.percent(15); }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
