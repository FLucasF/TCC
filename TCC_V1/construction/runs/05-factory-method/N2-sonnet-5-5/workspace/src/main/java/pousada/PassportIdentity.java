package pousada;

import java.util.List;
import java.util.Map;

public record PassportIdentity(String passport, String country) implements GuestIdentity {
    public static final IdentityType TYPE = new IdentityType() {
        public List<String> keys() { return List.of("guestPassport", "guestCountry"); }

        public GuestIdentity parse(RequestBody body) {
            return new PassportIdentity(body.text("guestPassport"), body.text("guestCountry"));
        }
    };

    public Map<String, String> fields() {
        return Map.of("guestPassport", passport, "guestCountry", country);
    }

    public ReservationDocument issue(DocumentData data) { return new BookingReceipt(data, this); }

    static final class BookingReceipt extends ReservationDocument {
        private final PassportIdentity guest;

        BookingReceipt(DocumentData data, PassportIdentity guest) {
            super(data, guest);
            this.guest = guest;
        }

        protected String title() { return "Booking receipt"; }

        protected DocumentLabels labels() { return DocumentLabels.ENGLISH; }

        protected List<String> guestLines() {
            return List.of("Guest: " + data.guestName()
                    + " (passport " + guest.passport() + ", " + guest.country() + ")");
        }
    }
}
