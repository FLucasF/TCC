package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class NonRefundablePolicy implements RatePlanPolicy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = PriceCalculator.countNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.subtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
