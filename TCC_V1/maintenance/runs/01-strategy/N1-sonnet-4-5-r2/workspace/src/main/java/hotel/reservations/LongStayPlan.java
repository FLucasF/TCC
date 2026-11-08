package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class LongStayPlan implements RatePlanStrategy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(NewReservation request, Room room) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public BigDecimal calculateDiscount(
            BigDecimal subtotal, NewReservation request, Room room, LocalDate checkIn, LocalDate checkOut) {
        return PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
