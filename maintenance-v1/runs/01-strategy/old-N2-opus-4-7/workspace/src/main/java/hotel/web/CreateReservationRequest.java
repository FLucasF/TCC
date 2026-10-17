package hotel.web;

import hotel.reservations.RatePlan;
import java.time.LocalDate;

/**
 * The JSON body of POST /reservations. Every field is required, except {@code companyCode}, which
 * only matters for the corporate rate plan and is ignored by the others.
 */
public record CreateReservationRequest(
        Integer roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer guests,
        RatePlan ratePlan,
        String companyCode) {
}
