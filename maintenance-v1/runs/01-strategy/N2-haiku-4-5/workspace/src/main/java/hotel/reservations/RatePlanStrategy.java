package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {
    Price calculate(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal refund(BigDecimal amountPaid);

    void validate(LocalDate checkIn, LocalDate checkOut, int nights);
}
