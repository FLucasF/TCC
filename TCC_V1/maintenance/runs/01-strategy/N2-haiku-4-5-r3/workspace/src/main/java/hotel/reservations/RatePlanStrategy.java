package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

interface RatePlanStrategy {
    void validate(ReservationData data) throws HotelException;

    Price calculate(Room room, ReservationData data);

    BigDecimal refundOnCancellation(Price price);
}
