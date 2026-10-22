package br.com.mare.pousada;

import java.util.List;
import java.util.Map;

public record PassportIdentity(String passport, String country) implements GuestIdentity {
    public static final IdentityKind KIND = new IdentityKind("PASSPORT", List.of("guestPassport", "guestCountry"),
            v -> new PassportIdentity(v.get("guestPassport"), v.get("guestCountry")));

    @Override
    public IdentityKind kind() {
        return KIND;
    }

    @Override
    public Map<String, String> values() {
        return Map.of("guestPassport", passport, "guestCountry", country);
    }

    @Override
    public Document document(Reservation reservation) {
        return new BookingReceiptDocument(reservation, this);
    }
}
