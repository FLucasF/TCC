package br.com.mare.plan;

import java.math.BigDecimal;

class NonRefundablePlan extends NightlyRatePlan {
    @Override
    public String code() { return "NON_REFUNDABLE"; }

    @Override
    protected BigDecimal discountRate(Booking booking) { return new BigDecimal("0.15"); }

    @Override
    protected BigDecimal refundRate() { return NONE; }
}
