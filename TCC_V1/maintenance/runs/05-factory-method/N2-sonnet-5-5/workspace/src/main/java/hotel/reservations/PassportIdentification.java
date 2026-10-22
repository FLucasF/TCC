package hotel.reservations;

import hotel.documents.BookingReceiptLayout;
import hotel.documents.DocumentLayout;

/** A foreign guest, identified by passport and country, who gets the document in English. */
public record PassportIdentification(String passport, String country) implements Identification {

    @Override
    public DocumentLayout layout() {
        return new BookingReceiptLayout();
    }
}
