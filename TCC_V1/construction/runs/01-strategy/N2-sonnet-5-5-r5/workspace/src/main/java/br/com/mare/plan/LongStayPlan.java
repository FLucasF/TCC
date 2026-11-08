package br.com.mare.plan;

import br.com.mare.domain.ApiException;

import java.math.BigDecimal;

class LongStayPlan extends NightlyRatePlan {
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public String code() { return "LONG_STAY"; }

    @Override
    protected void validate(Booking booking) {
        if (booking.nights() < MINIMUM_NIGHTS) {
            throw ApiException.invalid("O plano LONG_STAY exige pelo menos " + MINIMUM_NIGHTS + " noites");
        }
    }

    @Override
    protected BigDecimal discountRate(Booking booking) { return new BigDecimal("0.10"); }
}
