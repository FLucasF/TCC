package hotel.reservations;

import hotel.documents.BookingReceiptLayout;
import hotel.documents.DocumentLayout;
import java.util.Optional;

/** A foreign guest with a passport, who gets the receipt in English. */
public record PassportIdentification(String passport, String country) implements Identification {

    private static final DocumentLayout LAYOUT = new BookingReceiptLayout();

    static Optional<Identification> from(IdentificationFields fields) {
        return Identifications.ifAny(
                new PassportIdentification(fields.guestPassport(), fields.guestCountry()),
                fields.guestPassport(), fields.guestCountry());
    }

    @Override
    public IdentificationFields fields() {
        return new IdentificationFields(null, null, null, passport, country);
    }

    @Override
    public DocumentLayout documentLayout() {
        return LAYOUT;
    }
}
