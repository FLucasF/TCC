package hotel.reservations;

/** The person who stays. The name and e-mail are always the guest's; the identification varies. */
public record Guest(String name, String email, Identification identification) {

    public Guest(String name, String email, String cpf) {
        this(name, email, new Identification.Cpf(cpf));
    }

    public String cpf() {
        return identification instanceof Identification.Cpf cpf ? cpf.number() : null;
    }

    public String companyCnpj() {
        return identification instanceof Identification.Company company ? company.cnpj() : null;
    }

    public String companyName() {
        return identification instanceof Identification.Company company ? company.name() : null;
    }

    public String passport() {
        return identification instanceof Identification.Passport passport ? passport.number() : null;
    }

    public String country() {
        return identification instanceof Identification.Passport passport ? passport.country() : null;
    }
}
