package hotel.reservations;

/** A Brazilian guest, identified by CPF. */
public record CpfIdentification(String cpf) implements Identification {
}
