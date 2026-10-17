package hotel.reservations.rateplans;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
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

    private static final BigDecimal HALF = new BigDecimal("0.5");

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                || !request.checkIn().plusDays(2).equals(request.checkOut())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the weekend package goes from a Friday to the Sunday right after");
        }
    }

    @Override
    public Price price(NewReservation request, Room room, PriceCalculator pricing) {
        BigDecimal total = PriceCalculator.cents(PACKAGE_PRICE.get(room.type()));
        return new Price(2, total, PriceCalculator.cents(BigDecimal.ZERO), total);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid, Price price) {
        return PriceCalculator.cents(amountPaid.multiply(HALF));
    }
}
