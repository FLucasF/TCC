package pousada.domain;

import java.math.BigDecimal;

public class LongStayPlan extends NightlyRatePlan {
    private static final int MIN_NIGHTS = 7;

    @Override public String code() { return "LONG_STAY"; }

    @Override
    public void validate(Booking booking) {
        if (booking.nights() < MIN_NIGHTS) {
            throw ApiException.invalidRequest("O plano LONG_STAY exige " + MIN_NIGHTS + " noites ou mais");
        }
    }

    @Override protected BigDecimal discountRate(Booking booking) { return Money.percent(10); }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return fullRefund(amountPaid); }
}
