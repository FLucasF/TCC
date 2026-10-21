package hotel.reservations;

import hotel.documents.BookingReceiptLayout;
import hotel.documents.DocumentLayout;

/** A foreign guest, identified by passport. */
public record PassportIdentification(String passport, String country) implements Identification {

    @Override
    public DocumentLayout documentLayout() {
        return new BookingReceiptLayout();
    }
}
