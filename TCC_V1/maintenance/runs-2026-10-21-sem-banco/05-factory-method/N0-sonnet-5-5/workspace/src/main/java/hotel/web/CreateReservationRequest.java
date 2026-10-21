package hotel.web;

import hotel.reservations.RatePlan;
import java.time.LocalDate;

/** The JSON body of POST /reservations. Every field is required, except that the guest is identified in exactly one way:
 * guestCpf, or companyCnpj with companyName, or guestPassport with guestCountry. */
public record CreateReservationRequest(
        Integer roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        LocalDate checkIn,
        LocalDate checkOut,
        Integer guests,
        RatePlan ratePlan) {
}
