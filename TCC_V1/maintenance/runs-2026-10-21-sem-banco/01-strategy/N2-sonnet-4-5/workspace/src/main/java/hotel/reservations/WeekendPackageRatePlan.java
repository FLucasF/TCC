package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/** Weekend package (Friday to Sunday, exactly 2 nights) with fixed price; half refund on cancellation. */
final class WeekendPackageRatePlan implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must start on Friday");
        }
        if (request.checkOut().getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must end on Sunday");
        }
        long nights = java.time.temporal.ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package must be exactly 2 nights (Friday to Sunday)");
        }
    }

    @Override
    public Price calculatePrice(NewReservation request, Room room) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
        int nights = 2;
        BigDecimal discount = PriceHelper.cents(BigDecimal.ZERO);
        return new Price(nights, packagePrice, discount, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return PriceHelper.cents(reservation.amountPaid().multiply(new BigDecimal("0.50")));
    }
}
