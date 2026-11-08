package hotel.reservations;

import java.math.BigDecimal;

/** 15% off, but nothing comes back on cancellation. */
final class NonRefundablePlan extends DiscountedPlan {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    protected BigDecimal discountRate(NewReservation request, Price regularPrice) {
        return DISCOUNT;
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
