package hotel.reservations;

import java.math.BigDecimal;

/** 15% off the nights, but nothing comes back on cancellation. */
public final class NonRefundablePlan implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(Stay stay) {
        return PriceCalculator.nightsWithDiscount(stay, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
