package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import java.util.List;

/** The receipt in English, for a foreign guest with a passport. */
public final class BookingReceiptLayout extends DocumentLayout {

    public BookingReceiptLayout() {
        super(new EnglishWording());
    }

    @Override
    String title() {
        return "Booking receipt";
    }

    @Override
    List<String> partyLines(Reservation reservation) {
        Identification guest = reservation.guest().identification();
        return List.of(guestLine(reservation, "passport " + guest.passport() + ", " + guest.country()));
    }
}
