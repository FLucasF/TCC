package br.com.mealta.pousada;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estrangeiro com passaporte: recebe o booking receipt, em inglês. */
public record PassportIdentity(String passport, String country) implements Identity {
    public static final IdentityType TYPE = new IdentityType() {
        public List<String> fieldNames() { return List.of("guestPassport", "guestCountry"); }
        public Identity create(Map<String, String> v) {
            return new PassportIdentity(v.get("guestPassport"), v.get("guestCountry"));
        }
    };

    public Map<String, String> fields() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("guestPassport", passport);
        f.put("guestCountry", country);
        return f;
    }

    public String title() { return "Booking receipt"; }

    public String render(DocumentData d) {
        List<String> lines = new ArrayList<>();
        lines.add("Booking receipt no. " + d.reservationId());
        lines.add("Guest: " + d.guestName() + " (passport " + passport + ", " + country + ")");
        lines.add("Room: " + d.roomNumber() + " (" + d.roomType() + ")");
        lines.add("Check-in: " + d.checkIn() + " | Check-out: " + d.checkOut()
                + " | " + d.nights() + (d.nights() == 1 ? " night" : " nights"));
        lines.add("Nights: " + Money.brlEnglish(d.subtotal()));
        if (d.hasDiscount()) {
            lines.add("Discount: " + Money.brlEnglish(d.discount()));
        }
        lines.add("Total: " + Money.brlEnglish(d.total()));
        return String.join("\n", lines);
    }
}
