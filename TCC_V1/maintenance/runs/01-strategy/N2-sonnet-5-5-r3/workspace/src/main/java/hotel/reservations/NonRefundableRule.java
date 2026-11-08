package hotel.reservations;

import java.math.BigDecimal;

final class NonRefundableRule extends DiscountedRule {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    protected BigDecimal discountRate(NewReservation request) {
        return DISCOUNT;
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
