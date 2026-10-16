package hotel.reservations;

import java.math.BigDecimal;

/** 15% off the nights, open to everyone, but nothing comes back on cancellation. */
final class NonRefundablePlan extends NightlyDiscountPlan {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    BigDecimal discountRate(Stay stay) {
        return DISCOUNT;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
