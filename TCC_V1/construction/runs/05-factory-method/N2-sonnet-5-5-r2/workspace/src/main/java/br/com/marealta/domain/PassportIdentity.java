package br.com.marealta.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Booking receipt: o comprovante em inglês (BRL 1,234.56 e 2027-03-10). */
public final class PassportIdentity extends Identity {
    public static final IdentityKind KIND = new IdentityKind(List.of("guestPassport", "guestCountry"),
            f -> new PassportIdentity(f.get("guestPassport"), f.get("guestCountry")));

    private final String passport;
    private final String country;

    private PassportIdentity(String passport, String country) {
        this.passport = passport;
        this.country = country;
    }

    @Override
    public Map<String, String> fields() {
        return Map.of("guestPassport", passport, "guestCountry", country);
    }

    @Override
    protected String title() {
        return "Booking receipt";
    }

    @Override
    protected String numberMark() {
        return "no.";
    }

    @Override
    protected List<String> partyLines(ReceiptData d) {
        return List.of("Guest: " + d.guestName() + " (passport " + passport + ", " + country + ")");
    }

    @Override
    protected String roomLabel() {
        return "Room";
    }

    @Override
    protected String periodLine(ReceiptData d) {
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE;
        return "Check-in: " + d.checkIn().format(iso) + " | Check-out: " + d.checkOut().format(iso)
                + " | " + d.nights() + (d.nights() == 1 ? " night" : " nights");
    }

    @Override
    protected String amountLabel() {
        return "Nights";
    }

    @Override
    protected String discountLabel() {
        return "Discount";
    }

    @Override
    protected String totalLabel() {
        return "Total";
    }

    @Override
    protected String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        return "BRL " + new DecimalFormat("#,##0.00", symbols).format(value);
    }
}
