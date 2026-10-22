package hotel.reservations;

import hotel.documents.BookingReceiptLayout;
import hotel.documents.DocumentLayout;
import java.util.Optional;

/** A foreign guest, identified by passport and country. */
public record ForeignGuest(String passportNumber, String countryName) implements Identification {

    static Optional<Identification> from(IdentificationFields fields) {
        boolean hasPassport = IdentificationFields.isPresent(fields.passport());
        boolean hasCountry = IdentificationFields.isPresent(fields.country());
        if (!hasPassport && !hasCountry) {
            return Optional.empty();
        }
        if (!hasPassport || !hasCountry) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "a foreign guest needs both guestPassport and guestCountry");
        }
        return Optional.of(new ForeignGuest(fields.passport(), fields.country()));
    }

    @Override
    public DocumentLayout documentLayout() {
        return new BookingReceiptLayout();
    }

    @Override
    public String passport() {
        return passportNumber;
    }

    @Override
    public String country() {
        return countryName;
    }
}
