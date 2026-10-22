package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;

/** A plan that takes a percentage off the sum of the nights, for stays of a minimum number of nights. */
final class PercentageDiscountRule implements RatePlanRule {

    static final UnaryOperator<BigDecimal> EVERYTHING_BACK = paid -> paid;

    private final BigDecimal discountRate;
    private final int minimumNights;
    private final UnaryOperator<BigDecimal> refund;

    PercentageDiscountRule(BigDecimal discountRate, int minimumNights, UnaryOperator<BigDecimal> refund) {
        this.discountRate = discountRate;
        this.minimumNights = minimumNights;
        this.refund = refund;
    }

    @Override
    public Price price(Room room, NewReservation request, Price seasonal) {
        if (seasonal.nights() < minimumNights) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the rate plan " + request.ratePlan() + " needs at least " + minimumNights + " nights");
        }
        return seasonal.withDiscount(discountRate);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return refund.apply(amountPaid);
    }
}
