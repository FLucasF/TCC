package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Friday to Sunday, exactly two nights, for a closed price per room type that ignores the season. */
final class WeekendPackageRule implements RatePlanRule {

    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public Price price(Room room, NewReservation request, Price undiscounted) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                || !request.checkOut().equals(request.checkIn().plusDays(2))) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the Sunday after it");
        }
        BigDecimal closedPrice = PriceCalculator.cents(PRICE_BY_ROOM_TYPE.get(room.type()));
        return new Price(undiscounted.nights(), closedPrice, PriceCalculator.cents(BigDecimal.ZERO), closedPrice);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(reservation.amountPaid().multiply(REFUND_RATE));
    }
}
