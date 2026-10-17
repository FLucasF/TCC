package hotel.reservations.rateplans;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

final class LongStayPolicy implements RatePlanPolicy {

    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void validate(NewReservation request, Room room) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MIN_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long stay rate plan needs at least " + MIN_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(NewReservation request, Room room, PriceCalculator pricing) {
        return pricing.nightlyPlanPrice(room, request.checkIn(), request.checkOut(), DISCOUNT);
    }
}
