package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 15% off, but nothing comes back on cancellation. */
final class NonRefundableRule implements RatePlanRule {

    static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price standardPrice) {
        return RatePlanRule.withDiscount(standardPrice, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
