package pousada.domain;

import java.math.BigDecimal;

public class StandardPlan extends NightlyRatePlan {
    @Override public String code() { return "STANDARD"; }
    @Override protected BigDecimal discountRate(Booking booking) { return BigDecimal.ZERO; }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return fullRefund(amountPaid); }
}
