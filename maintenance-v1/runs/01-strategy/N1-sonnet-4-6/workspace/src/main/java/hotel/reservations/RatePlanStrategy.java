package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

interface RatePlanStrategy {
    Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);
    void validate(LocalDate checkIn, LocalDate checkOut, String companyCode);
    BigDecimal refund(Reservation reservation);
}
