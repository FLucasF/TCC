package hotel.reservations;

import java.math.BigDecimal;

/** The plain plan: the nights at full price, and everything back on cancellation. */
final class StandardPlan implements RatePlanPolicy {

    @Override
    public Price price(Stay stay) {
        return Price.withoutDiscount(stay.nights(), stay.nightsSubtotal());
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
