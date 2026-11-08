package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Long stay: 7 nights or more, 10% discount, high season still applies to high-season nights. */
public final class LongStayRatePlan implements RatePlanStrategy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);
        int nights = calculator.countNights(checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
