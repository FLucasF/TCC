package com.pousadamarealta.model;

public class GuestIdentification {
    private final String guestCpf;
    private final String companyCnpj;
    private final String companyName;
    private final String guestPassport;
    private final String guestCountry;

    private GuestIdentification(String guestCpf, String companyCnpj, String companyName,
                                String guestPassport, String guestCountry) {
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
    }

    public static GuestIdentification withCpf(String cpf) {
        return new GuestIdentification(cpf, null, null, null, null);
    }

    public static GuestIdentification withCompany(String cnpj, String companyName) {
        return new GuestIdentification(null, cnpj, companyName, null, null);
    }

    public static GuestIdentification withPassport(String passport, String country) {
        return new GuestIdentification(null, null, null, passport, country);
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public boolean isCpf() {
        return guestCpf != null;
    }

    public boolean isCompany() {
        return companyCnpj != null;
    }

    public boolean isPassport() {
        return guestPassport != null;
    }
}
