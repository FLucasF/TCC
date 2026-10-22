package hotel.reservations;

import hotel.documents.BookingReceiptLayout;
import hotel.documents.DocumentLayout;

/** A foreign guest, identified by passport. */
public record PassportIdentification(String passport, String country) implements Identification {

    static final String TYPE = "PASSPORT";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String number() {
        return passport;
    }

    @Override
    public String detail() {
        return country;
    }

    @Override
    public DocumentLayout layout() {
        return new BookingReceiptLayout();
    }
}
