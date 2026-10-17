package hotel.reservations;

import java.math.BigDecimal;

/** A plan priced from the nights of the stay, with a discount on their sum. */
abstract class DiscountedNightsPlan implements RatePlanPolicy {

    @Override
    public Price price(Stay stay) {
        BigDecimal subtotal = PriceCalculator.nightsSubtotal(stay);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate(stay)));
        return new Price(stay.nights(), subtotal, discount, subtotal.subtract(discount));
    }

    /** How much of the nights' sum the plan takes off. */
    abstract BigDecimal discountRate(Stay stay);
}
