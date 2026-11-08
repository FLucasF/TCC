package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class LongStayRatePlan extends StandardRatePlan {

    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");

    @Override
    public void validate(ReservationData data) throws HotelException {
        int nights = 0;
        var night = data.checkIn();
        while (night.isBefore(data.checkOut())) {
            nights++;
            night = night.plusDays(1);
        }

        if (nights < 7) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "long stay requires at least 7 nights");
        }
    }

    @Override
    public Price calculate(Room room, ReservationData data) {
        Price standardPrice = super.calculate(room, data);
        BigDecimal discount = cents(standardPrice.subtotal().multiply(LONG_STAY_DISCOUNT));
        return new Price(
                standardPrice.nights(),
                standardPrice.subtotal(),
                discount,
                standardPrice.subtotal().subtract(discount));
    }

    @Override
    public BigDecimal refundOnCancellation(Price price) {
        return price.total();
    }
}
