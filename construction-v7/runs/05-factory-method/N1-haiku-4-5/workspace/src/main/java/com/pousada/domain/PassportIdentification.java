package com.pousada.domain;

public final class PassportIdentification implements GuestIdentification {
    public String guestPassport;
    public String guestCountry;

    public PassportIdentification() {}
    public PassportIdentification(String guestPassport, String guestCountry) {
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
    }
}
