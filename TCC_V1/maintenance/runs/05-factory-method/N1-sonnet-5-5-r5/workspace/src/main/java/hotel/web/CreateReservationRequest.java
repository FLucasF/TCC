package hotel.web;

import hotel.reservations.Identification;
import hotel.reservations.RatePlan;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The JSON body of POST /reservations. Every field is required, except that the guest is identified in one
 * of three ways: guestCpf, or companyCnpj with companyName, or guestPassport with guestCountry.
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

    /** The way the guest is identified; empty unless exactly one way is given, whole. */
    Optional<Identification> identification() {
        int filledFields = 0;
        for (String field : new String[] {guestCpf, companyCnpj, companyName, guestPassport, guestCountry}) {
            filledFields += filled(field) ? 1 : 0;
        }
        if (filledFields == 1 && filled(guestCpf)) {
            return Optional.of(new Identification.Cpf(guestCpf));
        }
        if (filledFields == 2 && filled(companyCnpj) && filled(companyName)) {
            return Optional.of(new Identification.Company(companyCnpj, companyName));
        }
        if (filledFields == 2 && filled(guestPassport) && filled(guestCountry)) {
            return Optional.of(new Identification.Passport(guestPassport, guestCountry));
        }
        return Optional.empty();
    }

    private static boolean filled(String text) {
        return text != null && !text.isBlank();
    }
}
