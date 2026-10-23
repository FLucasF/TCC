package com.marealta.domain.model;

public record CompanyGuest(
    String guestName,
    String guestEmail,
    String companyCnpj,
    String companyName
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
