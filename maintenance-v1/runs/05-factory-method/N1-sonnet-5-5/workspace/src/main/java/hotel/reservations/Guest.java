package hotel.reservations;

public record Guest(String name, String email, Identification identification) {

    public Guest(String name, String email, String cpf) {
        this(name, email, new Identification.Cpf(cpf));
    }

    /** Null when the guest is not identified by CPF. */
    public String cpf() {
        return identification instanceof Identification.Cpf cpf ? cpf.number() : null;
    }

    /** Null when the reservation is not made by a company. */
    public String companyCnpj() {
        return identification instanceof Identification.Company company ? company.cnpj() : null;
    }

    public String companyName() {
        return identification instanceof Identification.Company company ? company.name() : null;
    }

    /** Null when the guest is not a foreigner. */
    public String passport() {
        return identification instanceof Identification.Foreigner foreigner ? foreigner.passport() : null;
    }

    public String country() {
        return identification instanceof Identification.Foreigner foreigner ? foreigner.country() : null;
    }
}
