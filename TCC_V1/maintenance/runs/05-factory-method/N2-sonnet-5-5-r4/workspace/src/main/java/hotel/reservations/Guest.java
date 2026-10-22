package hotel.reservations;

public record Guest(String name, String email, Identification identification) {

    /** A Brazilian guest, identified by CPF. */
    public Guest(String name, String email, String cpf) {
        this(name, email, new CpfIdentification(cpf));
    }
}
