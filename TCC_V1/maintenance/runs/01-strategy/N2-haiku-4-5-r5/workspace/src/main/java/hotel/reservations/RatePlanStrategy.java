package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

interface RatePlanStrategy {
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, Optional<String> companyCode);

    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, Optional<String> companyCode);

    BigDecimal calculateRefund(Price price, ReservationStatus status);
}
