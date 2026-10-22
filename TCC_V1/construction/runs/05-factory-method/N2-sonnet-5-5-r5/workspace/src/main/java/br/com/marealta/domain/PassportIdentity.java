package br.com.marealta.domain;

public record PassportIdentity(String passport, String country) implements GuestIdentity {
    public static final IdentityKind KIND = new IdentityKind() {
        @Override
        public boolean isPresent(IdentityFields f) {
            return f.guestPassport() != null || f.guestCountry() != null;
        }

        @Override
        public GuestIdentity create(IdentityFields f) {
            return new PassportIdentity(IdentityKind.require(f.guestPassport(), "guestPassport"),
                    IdentityKind.require(f.guestCountry(), "guestCountry"));
        }
    };

    @Override
    public IdentityFields fields() {
        return new IdentityFields(null, null, null, passport, country);
    }

    @Override
    public ReservationDocument documentFor(Reservation reservation) {
        return new BookingReceiptDocument(reservation, passport, country);
    }
}
