package pousada.domain.identification;

import pousada.domain.document.Document;
import pousada.domain.document.EnglishLayout;
import pousada.domain.document.Voucher;

import java.util.List;
import java.util.Map;

/** Estrangeiro com passaporte: recebe o booking receipt, em inglês. */
record PassportIdentification(String passport, String country) implements Identification {

    static final String TITLE = "Booking receipt";

    @Override
    public Map<String, String> fields() {
        return Map.of("guestPassport", passport, "guestCountry", country);
    }

    @Override
    public Document issue(Voucher v) {
        String text = EnglishLayout.render(TITLE, v,
                List.of("Guest: " + v.guestName() + " (passport " + passport + ", " + country + ")"), List.of());
        return new Document(TITLE, v, IdentificationKinds.withNulls(this), text);
    }

    static final class Kind implements IdentificationKind {
        @Override
        public List<String> fieldNames() {
            return List.of("guestPassport", "guestCountry");
        }

        @Override
        public Identification create(Map<String, String> values) {
            return new PassportIdentification(values.get("guestPassport"), values.get("guestCountry"));
        }
    }
}
