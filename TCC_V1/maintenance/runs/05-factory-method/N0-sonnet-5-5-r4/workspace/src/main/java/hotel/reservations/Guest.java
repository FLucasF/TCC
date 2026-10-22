package hotel.reservations;

/**
 * The guest who will stay, and how the reservation identifies who it is for: a CPF, a company (CNPJ and
 * company name), or a foreign guest (passport and country). Exactly one of the three is filled in.
 */
public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String passport,
        String country) {

    /** A Brazilian guest, identified by CPF. */
    public Guest(String name, String email, String cpf) {
        this(name, email, cpf, null, null, null, null);
    }

    public static Guest ofCompany(String name, String email, String cnpj, String companyName) {
        return new Guest(name, email, null, cnpj, companyName, null, null);
    }

    public static Guest ofForeigner(String name, String email, String passport, String country) {
        return new Guest(name, email, null, null, null, passport, country);
    }
}
