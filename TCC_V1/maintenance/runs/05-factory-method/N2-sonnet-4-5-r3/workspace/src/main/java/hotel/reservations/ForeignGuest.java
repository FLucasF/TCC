package hotel.reservations;

public record ForeignGuest(String name, String email, String passport, String country) implements Guest {
}
