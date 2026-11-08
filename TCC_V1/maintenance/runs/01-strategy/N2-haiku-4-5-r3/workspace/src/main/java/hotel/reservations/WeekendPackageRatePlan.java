package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;

final class WeekendPackageRatePlan implements RatePlanStrategy {

    @Override
    public void validate(ReservationData data) throws HotelException {
        int nights = 0;
        var night = data.checkIn();
        while (night.isBefore(data.checkOut())) {
            nights++;
            night = night.plusDays(1);
        }

        if (nights != 2) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package requires exactly 2 nights (Friday to Sunday)");
        }

        if (data.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "weekend package check-in must be on a Friday");
        }
    }

    @Override
    public Price calculate(Room room, ReservationData data) {
        BigDecimal total = getPackagePrice(room.type());
        return new Price(2, total, PriceCalculator.cents(BigDecimal.ZERO), total);
    }

    @Override
    public BigDecimal refundOnCancellation(Price price) {
        return PriceCalculator.cents(price.total().divide(new BigDecimal("2"), java.math.RoundingMode.HALF_EVEN));
    }

    private BigDecimal getPackagePrice(RoomType roomType) {
        return switch (roomType) {
            case STANDARD -> new BigDecimal("520.00");
            case DELUXE -> new BigDecimal("820.00");
            case SUITE -> new BigDecimal("1200.00");
        };
    }
}
