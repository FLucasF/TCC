package pousada.document;

import java.util.List;
import java.util.Map;

/** Booking receipt, o comprovante em inglês para estrangeiros com passaporte. */
final class PassportReceipt extends DocumentType {
    @Override public String code() { return "PASSPORT"; }
    @Override public List<String> fields() { return List.of("guestPassport", "guestCountry"); }
    @Override public String title() { return "Booking receipt"; }
    @Override protected Language language() { return Language.EN; }

    @Override
    protected List<String> identityLines(String guestName, Map<String, String> identity) {
        return List.of("Guest: " + guestName + " (passport " + identity.get("guestPassport")
                + ", " + identity.get("guestCountry") + ")");
    }
}
