package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Friday to Sunday (two nights) for a closed price all year round; half comes back on cancellation. */
final class WeekendPackagePlan implements RatePlanPolicy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public Price price(Room room, NewReservation request, Price regularPrice) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.checkOut().equals(request.checkIn().plusDays(2));
        if (!fridayToSunday) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the weekend package is only from a Friday to the following Sunday");
        }
        BigDecimal packagePrice = PACKAGE_PRICE.get(room.type());
        return new Price(regularPrice.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(reservation.amountPaid().multiply(REFUND_RATE));
    }
}
