package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

final class WeekendPackagePolicy implements RatePlanPolicy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                || request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY
                || !request.checkOut().equals(request.checkIn().plusDays(2))) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from Friday to the following Sunday (exactly two nights)");
        }
    }

    @Override
    public Price calculate(Room room, NewReservation request) {
        BigDecimal price = PriceCalculator.cents(PACKAGE_PRICE.get(room.type()));
        return new Price(2, price, PriceCalculator.cents(BigDecimal.ZERO), price);
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return PriceCalculator.cents(
                reservation.amountPaid().divide(new BigDecimal("2"), java.math.RoundingMode.HALF_EVEN));
    }
}
