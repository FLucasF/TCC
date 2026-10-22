package hotel.reservations;

/** An individual guest identified by CPF. */
public record IndividualGuest(String name, String email, String cpf) implements Guest {
}
