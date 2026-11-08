package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;

final class WeekendPackageRatePlan implements RatePlanStrategy {

    private static final java.util.Map<RoomType, BigDecimal> PRICES = java.util.Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must start on Friday");
        }
        if (request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must end on Sunday");
        }
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights != 2) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "weekend package must be exactly 2 nights");
        }
    }

    @Override
    public Price calculatePrice(NewReservation request, Room room) {
        BigDecimal price = PRICES.get(room.type());
        return new Price(2, price, PriceCalculator.cents(BigDecimal.ZERO), price);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return PriceCalculator.cents(reservation.amountPaid().divide(new BigDecimal("2"), java.math.RoundingMode.HALF_EVEN));
    }
}
