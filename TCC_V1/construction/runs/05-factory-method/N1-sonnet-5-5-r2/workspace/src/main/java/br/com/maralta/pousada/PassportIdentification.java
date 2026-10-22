package br.com.maralta.pousada;

import java.util.List;
import java.util.Map;

public record PassportIdentification(String passport, String country) implements Identification {
    static final Identifications.Kind KIND = new Identifications.Kind("PASSPORT",
            List.of("guestPassport", "guestCountry"),
            f -> new PassportIdentification(f.get("guestPassport"), f.get("guestCountry")));

    @Override
    public Map<String, String> fields() {
        return Map.of("guestPassport", passport, "guestCountry", country);
    }

    @Override
    public Language language() {
        return Language.EN;
    }

    @Override
    public String title() {
        return "Booking receipt";
    }

    @Override
    public List<String> partyLines(String guestName) {
        return List.of("Guest: " + guestName + " (passport " + passport + ", " + country + ")");
    }
}
