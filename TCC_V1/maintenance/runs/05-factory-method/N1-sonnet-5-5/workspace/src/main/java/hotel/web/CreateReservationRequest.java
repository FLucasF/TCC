package hotel.web;

import hotel.reservations.RatePlan;
import java.time.LocalDate;

/** The JSON body of POST /reservations. Every field is required, except the identification: the CPF, or the
 * CNPJ with the company name, or the passport with the country. */
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
