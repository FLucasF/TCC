package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {

    void validate(NewReservation request, Room room);

    Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, NewReservation request);

    BigDecimal refundAmount(BigDecimal amountPaid);
}
