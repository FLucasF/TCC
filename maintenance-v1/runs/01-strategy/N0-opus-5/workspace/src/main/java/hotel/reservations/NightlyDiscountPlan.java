package hotel.reservations;

import java.math.BigDecimal;

/**
 * A plan priced from the room's nightly rates (high season included), with a discount on their sum.
 * Cancelling gives everything back, unless the plan says otherwise.
 */
abstract class NightlyDiscountPlan implements RatePlanPolicy {

    @Override
    public Price price(Stay stay) {
        BigDecimal subtotal = PriceCalculator.nightsSubtotal(stay);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate(stay)));
        return new Price(stay.nights(), subtotal, discount, subtotal.subtract(discount));
    }

    /** The share of the nights that comes off, between 0 and 1. */
    abstract BigDecimal discountRate(Stay stay);

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
