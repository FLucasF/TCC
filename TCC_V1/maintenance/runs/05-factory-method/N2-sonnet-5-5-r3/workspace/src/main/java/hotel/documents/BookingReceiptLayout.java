package hotel.documents;

import hotel.reservations.Guest;

/** The receipt in English, for a foreign guest. */
public final class BookingReceiptLayout extends DocumentLayout {

    public BookingReceiptLayout() {
        super("Booking receipt", Wording.ENGLISH);
    }

    @Override
    String identification(Guest guest) {
        return "Guest: " + guest.name() + " (passport " + guest.identification().passport()
                + ", " + guest.identification().country() + ")\n";
    }
}
