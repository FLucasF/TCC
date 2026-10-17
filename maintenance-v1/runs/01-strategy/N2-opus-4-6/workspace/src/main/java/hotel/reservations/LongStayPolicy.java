package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class LongStayPolicy implements RatePlanPolicy {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (PriceCalculator.countNights(checkIn, checkOut) < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long stay plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = PriceCalculator.countNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.subtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
