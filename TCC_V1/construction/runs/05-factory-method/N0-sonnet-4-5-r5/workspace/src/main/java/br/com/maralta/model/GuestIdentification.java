package br.com.maralta.model;

public class GuestIdentification {
    private String guestCpf;
    private String companyCnpj;
    private String companyName;
    private String guestPassport;
    private String guestCountry;

    public GuestIdentification() {}

    public static GuestIdentification withCpf(String cpf) {
        GuestIdentification id = new GuestIdentification();
        id.guestCpf = cpf;
        return id;
    }

    public static GuestIdentification withCompany(String cnpj, String companyName) {
        GuestIdentification id = new GuestIdentification();
        id.companyCnpj = cnpj;
        id.companyName = companyName;
        return id;
    }

    public static GuestIdentification withPassport(String passport, String country) {
        GuestIdentification id = new GuestIdentification();
        id.guestPassport = passport;
        id.guestCountry = country;
        return id;
    }

    public boolean isValid() {
        int count = 0;
        if (guestCpf != null) count++;
        if (companyCnpj != null && companyName != null) count++;
        if (guestPassport != null && guestCountry != null) count++;
        return count == 1;
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

    public String getGuestCpf() {
        return guestCpf;
    }

    public void setGuestCpf(String guestCpf) {
        this.guestCpf = guestCpf;
    }

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public void setCompanyCnpj(String companyCnpj) {
        this.companyCnpj = companyCnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public void setGuestPassport(String guestPassport) {
        this.guestPassport = guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public void setGuestCountry(String guestCountry) {
        this.guestCountry = guestCountry;
    }
}
