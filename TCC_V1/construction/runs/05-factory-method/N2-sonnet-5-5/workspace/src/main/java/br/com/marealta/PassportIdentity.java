package br.com.marealta;

import java.util.List;

public class PassportIdentity implements IdentityType {
    public static final String PASSPORT = "guestPassport";
    public static final String COUNTRY = "guestCountry";

    public String kind() { return "PASSPORT"; }

    public List<String> fields() { return List.of(PASSPORT, COUNTRY); }

    public DocumentFormat format() { return new BookingReceiptFormat(); }
}
