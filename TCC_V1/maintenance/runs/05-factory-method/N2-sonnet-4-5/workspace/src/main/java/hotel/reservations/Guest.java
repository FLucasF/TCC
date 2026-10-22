package hotel.reservations;

/** The guest's identification: individual (CPF), company (CNPJ), or foreign guest (passport). */
public final class Guest {

    private final String name;
    private final String email;
    private final String cpf;
    private final String companyCnpj;
    private final String companyName;
    private final String passport;
    private final String country;

    private Guest(
            String name,
            String email,
            String cpf,
            String companyCnpj,
            String companyName,
            String passport,
            String country) {
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.passport = passport;
        this.country = country;
    }

    public static Guest withCpf(String name, String email, String cpf) {
        return new Guest(name, email, cpf, null, null, null, null);
    }

    public static Guest withCompany(String name, String email, String companyCnpj, String companyName) {
        return new Guest(name, email, null, companyCnpj, companyName, null, null);
    }

    public static Guest withPassport(String name, String email, String passport, String country) {
        return new Guest(name, email, null, null, null, passport, country);
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String cpf() {
        return cpf;
    }

    public String companyCnpj() {
        return companyCnpj;
    }

    public String companyName() {
        return companyName;
    }

    public String passport() {
        return passport;
    }

    public String country() {
        return country;
    }
}
