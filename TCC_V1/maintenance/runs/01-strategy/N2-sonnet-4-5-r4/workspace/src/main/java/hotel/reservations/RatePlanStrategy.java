package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

interface RatePlanStrategy {

    void validate(NewReservation request, Room room);

    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
