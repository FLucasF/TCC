package hotel.reservations;

import java.math.BigDecimal;

final class NonRefundablePolicy implements RatePlanPolicy {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(Stay stay, Price fullPrice) {
        return fullPrice.withDiscountRate(DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
