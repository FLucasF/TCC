package hotel.reservations;

import java.time.LocalDate;

public record ReservationData(
        int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {}
