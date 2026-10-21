package hotel.web;

import hotel.reservations.RatePlan;
import java.time.LocalDate;

/** The JSON body of POST /reservations. Most fields are required; companyCode is only for CORPORATE rate plan. */
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
