package pousada.identity;

import java.util.List;
import java.util.Map;

public record PassportIdentity(String passport, String country) implements GuestIdentity {

    @Override
    public IdentityKind kind() {
        return IdentityKind.PASSPORT;
    }

    @Override
    public Map<String, String> fields() {
        return Map.of("guestPassport", passport, "guestCountry", country);
    }

    @Override
    public DocumentLanguage language() {
        return DocumentLanguage.ENGLISH;
    }

    @Override
    public String documentTitle() {
        return "Booking receipt";
    }

    @Override
    public List<String> partyLines(String guestName) {
        return List.of("Guest: " + guestName + " (passport " + passport + ", " + country + ")");
    }
}
