package hotel.reservations;

/**
 * The guest who will stay, and how they are identified: by CPF, or as a company's guest (CNPJ and company
 * name), or as a foreigner (passport and country). Exactly one of the three is filled; the rest are null.
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
}
