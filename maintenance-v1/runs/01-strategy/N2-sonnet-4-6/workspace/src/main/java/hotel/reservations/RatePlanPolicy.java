package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

interface RatePlanPolicy {
    /** Throws INVALID_REQUEST if the plan cannot be used for this room and stay. */
    void validate(Room room, LocalDate checkIn, LocalDate checkOut);

    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
