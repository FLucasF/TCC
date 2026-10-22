package hotel.reservations;

public record PersonGuest(String name, String email, String cpf) implements Guest {
}
