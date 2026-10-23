package com.marealta.domain.model;

public record PassportGuest(
    String guestName,
    String guestEmail,
    String guestPassport,
    String guestCountry
) implements GuestIdentification {

    @Override
    public String getGuestName() {
        return guestName;
    }

    @Override
    public String getGuestEmail() {
        return guestEmail;
    }
}
