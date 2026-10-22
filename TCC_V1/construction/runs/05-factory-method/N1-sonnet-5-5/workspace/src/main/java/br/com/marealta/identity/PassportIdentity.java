package br.com.marealta.identity;

import java.util.List;
import java.util.Map;

final class PassportIdentity implements IdentityKind {
    public String code() { return "PASSPORT"; }

    public List<String> fieldNames() { return List.of("guestPassport", "guestCountry"); }

    public String documentTitle() { return "Booking receipt"; }

    public Language language() { return Language.EN; }

    public List<String> identityLines(String guestName, Map<String, String> values) {
        return List.of("Guest: " + guestName + " (passport " + values.get("guestPassport") + ", "
                + values.get("guestCountry") + ")");
    }
}
