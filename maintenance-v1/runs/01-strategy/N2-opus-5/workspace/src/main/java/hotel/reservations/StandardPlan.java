package hotel.reservations;

import java.math.BigDecimal;

/** The plain plan: the nights at full price, and everything paid comes back on cancellation. */
public final class StandardPlan implements RatePlanPolicy {

    @Override
    public Price price(Stay stay) {
        return PriceCalculator.nightsWithDiscount(stay, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
