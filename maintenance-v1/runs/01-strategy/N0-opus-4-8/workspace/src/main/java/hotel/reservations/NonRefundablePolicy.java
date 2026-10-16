package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 15% off the nights, but nothing comes back on cancellation. */
final class NonRefundablePolicy implements RatePlanPolicy {

    private static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator pricing) {
        BigDecimal subtotal = pricing.nightsSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT));
        return new Price(PriceCalculator.nights(checkIn, checkOut), subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
