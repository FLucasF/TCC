package hotel.web;

import hotel.reservations.RatePlan;
import java.time.LocalDate;

/** The JSON body of POST /reservations. Every field is required, except companyCode (only the corporate rate uses it). */
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
