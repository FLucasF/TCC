package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;

/** The receipt, in English, of a foreign guest identified by a passport. */
final class BookingReceiptLayout extends DocumentLayout {

    BookingReceiptLayout() {
        super("Booking receipt", DocumentLanguage.ENGLISH);
    }

    @Override
    String guestLines(Reservation reservation) {
        Identification passport = reservation.guest().identification();
        return guestName(reservation) + " (passport " + passport.passport() + ", " + passport.country() + ")\n";
    }
}
