package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The long-stay rate, for seven nights or more: 10% off the nights, with the high-season surcharge
 * still applying on high-season nights. Fewer than seven nights cannot take it. Cancelling gives
 * everything back.
 */
final class LongStayPolicy implements RatePlanPolicy {

    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (PriceCalculator.nights(checkIn, checkOut) < MIN_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the long-stay rate needs at least " + MIN_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator pricing) {
        BigDecimal subtotal = pricing.nightsSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT));
        return new Price(PriceCalculator.nights(checkIn, checkOut), subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
