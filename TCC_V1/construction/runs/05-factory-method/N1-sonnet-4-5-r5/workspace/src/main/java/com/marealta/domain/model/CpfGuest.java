package com.marealta.domain.model;

public record CpfGuest(
    String guestName,
    String guestEmail,
    String guestCpf
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
