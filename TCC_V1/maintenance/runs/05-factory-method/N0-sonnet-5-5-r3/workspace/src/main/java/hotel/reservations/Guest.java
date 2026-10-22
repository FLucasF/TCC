package hotel.reservations;

/**
 * The guest who will stay, and how they are identified: by a CPF, or as a company's employee (the company's
 * CNPJ and name), or as a foreigner (passport and country). Exactly one of the three is filled in; the
 * fields of the other two are null.
 */
public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String passport,
        String country) {

    public Guest(String name, String email, String cpf) {
        this(name, email, cpf, null, null, null, null);
    }

    public static Guest ofCompany(String name, String email, String cnpj, String companyName) {
        return new Guest(name, email, null, cnpj, companyName, null, null);
    }

    public static Guest ofForeigner(String name, String email, String passport, String country) {
        return new Guest(name, email, null, null, null, passport, country);
    }

    public boolean isCompany() {
        return companyCnpj != null;
    }

    public boolean isForeigner() {
        return passport != null;
    }
}
