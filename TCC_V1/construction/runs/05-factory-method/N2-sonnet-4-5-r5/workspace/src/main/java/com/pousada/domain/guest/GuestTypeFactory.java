package com.pousada.domain.guest;

public final class GuestTypeFactory {

    public static GuestType create(
            String guestCpf,
            String companyCnpj,
            String companyName,
            String guestPassport,
            String guestCountry) {

        int typeCount = 0;
        GuestType result = null;

        if (guestCpf != null) {
            typeCount++;
            result = new IndividualGuest(guestCpf);
        }

        if (companyCnpj != null && companyName != null) {
            typeCount++;
            result = new CorporateGuest(companyCnpj, companyName);
        }

        if (guestPassport != null && guestCountry != null) {
            typeCount++;
            result = new ForeignGuest(guestPassport, guestCountry);
        }

        if (typeCount == 0 || typeCount > 1) {
            throw new IllegalArgumentException("Exactly one guest type must be provided");
        }

        return result;
    }
}
