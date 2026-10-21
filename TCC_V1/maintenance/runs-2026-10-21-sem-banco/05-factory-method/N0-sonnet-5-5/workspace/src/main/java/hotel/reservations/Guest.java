package hotel.reservations;

/** The guest who will stay. For a company booking, the company is in the identification. */
public record Guest(String name, String email, Identification identification) {

    public Guest(String name, String email, String cpf) {
        this(name, email, new Identification.Cpf(cpf));
    }
}
