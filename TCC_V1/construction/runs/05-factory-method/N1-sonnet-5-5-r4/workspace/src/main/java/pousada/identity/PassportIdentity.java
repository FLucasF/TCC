package pousada.identity;

import pousada.document.IssuedDocument;
import pousada.document.Stay;
import pousada.document.Wording;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PassportIdentity(String passport, String country) implements Identity {
    private static final String TITLE = "Booking receipt";

    public static final IdentityKind KIND = new IdentityKind() {
        @Override
        public String name() {
            return "PASSPORT";
        }

        @Override
        public List<String> fieldNames() {
            return List.of("guestPassport", "guestCountry");
        }

        @Override
        public Identity create(Map<String, String> fields) {
            return new PassportIdentity(Fields.required(fields, "guestPassport"),
                    Fields.required(fields, "guestCountry"));
        }
    };

    @Override
    public IdentityKind kind() {
        return KIND;
    }

    @Override
    public Map<String, String> fields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("guestPassport", passport);
        fields.put("guestCountry", country);
        return fields;
    }

    @Override
    public IssuedDocument issue(Stay stay) {
        String guest = "Guest: " + stay.guestName() + " (passport " + passport + ", " + country + ")";
        return new IssuedDocument(TITLE, Wording.ENGLISH.render(TITLE, stay, List.of(guest), List.of()));
    }
}
