package hotel.web;

import hotel.reservations.RatePlan;
import java.time.LocalDate;

/**
 * The JSON body of POST /reservations.
 * Required fields: roomNumber, guestName, guestEmail, checkIn, checkOut, guests, ratePlan.
 * Guest identification: exactly one of (guestCpf) OR (companyCnpj AND companyName) OR (guestPassport AND guestCountry).
 */
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
