package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

import static hotel.reservations.PriceCalculator.cents;

/** 10% off for stays of 7 nights or more; full refund on cancellation. */
final class LongStayRatePlan extends BaseRatePlanStrategy {

    private static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.10");

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = countNights(checkIn, checkOut);
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay rate plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = countNights(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = cents(subtotal.multiply(DISCOUNT_PERCENTAGE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
