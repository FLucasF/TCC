package com.pousadamarealta.domain;

public class PassportIdentification implements GuestIdentification {
    private final String passport;
    private final String country;

    public PassportIdentification(String passport, String country) {
        this.passport = passport;
        this.country = country;
    }

    public String getPassport() {
        return passport;
    }

    public String getCountry() {
        return country;
    }

    @Override
    public String formatForDocument() {
        return "passport " + passport + ", " + country;
    }
}
