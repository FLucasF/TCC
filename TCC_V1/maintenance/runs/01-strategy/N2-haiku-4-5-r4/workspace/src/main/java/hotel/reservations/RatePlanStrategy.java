package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

interface RatePlanStrategy {
    void validate(NewReservation request, Room room);

    Price calculatePrice(NewReservation request, Room room);

    BigDecimal calculateRefund(Reservation reservation);
}
