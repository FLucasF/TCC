package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

interface RatePlanStrategy {

    void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
