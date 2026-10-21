package pousada.identity;

import java.util.List;
import java.util.Map;

public record PassportIdentity(String passport, String country) implements GuestIdentity {
    static final String TITLE = "Booking receipt";

    static final IdentityKind KIND = new IdentityKind(List.of("guestPassport", "guestCountry"),
            values -> new PassportIdentity(values.get("guestPassport"), values.get("guestCountry")));

    @Override
    public Map<String, String> fields() {
        return Map.of("guestPassport", passport, "guestCountry", country);
    }

    @Override
    public String documentTitle() {
        return TITLE;
    }

    @Override
    public String renderDocument(DocumentData data) {
        return DocumentLayout.render(DocumentLayout.ENGLISH, TITLE, data,
                List.of("Guest: " + data.guestName() + " (passport " + passport + ", " + country + ")"), List.of());
    }
}
