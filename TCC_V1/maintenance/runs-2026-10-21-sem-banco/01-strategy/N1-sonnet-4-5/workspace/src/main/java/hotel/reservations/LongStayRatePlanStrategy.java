package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Long stay: 7 nights or more, 10% discount, full refund on cancellation. */
final class LongStayRatePlanStrategy implements RatePlanStrategy {

    private static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (PriceCalculator.countNights(checkIn, checkOut) < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay rate plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal subtotal = PriceCalculator.sumNights(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(
                PriceCalculator.countNights(checkIn, checkOut), subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
