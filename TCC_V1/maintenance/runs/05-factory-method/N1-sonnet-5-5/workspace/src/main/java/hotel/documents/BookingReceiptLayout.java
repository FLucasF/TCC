package hotel.documents;

import hotel.reservations.Reservation;

/** The booking receipt, in English, of the foreign guest. */
final class BookingReceiptLayout extends DocumentLayout {

    BookingReceiptLayout() {
        super(ENGLISH);
    }

    @Override
    String title() {
        return "Booking receipt";
    }

    @Override
    String guestLines(Reservation reservation) {
        var identification = reservation.guest().identification();
        return "Guest: " + reservation.guest().name() + " (passport " + identification.passport() + ", "
                + identification.country() + ")\n";
    }
}
