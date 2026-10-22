package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import java.util.List;

/** The receipt in English, for the foreign guest who has a passport instead of a CPF. */
final class BookingReceiptLayout extends DocumentLayout {

    @Override
    String title() {
        return "Booking receipt";
    }

    @Override
    Language language() {
        return Language.ENGLISH;
    }

    @Override
    List<String> identityLines(Reservation reservation) {
        Identification passport = reservation.guest().identification();
        return List.of("Guest: " + reservation.guest().name()
                + " (passport " + passport.passportNumber() + ", " + passport.passportCountry() + ")");
    }
}
