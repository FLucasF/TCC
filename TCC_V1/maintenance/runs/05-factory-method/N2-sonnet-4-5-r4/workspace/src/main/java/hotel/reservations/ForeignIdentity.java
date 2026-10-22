package hotel.reservations;

public record ForeignIdentity(String passport, String country) implements GuestIdentity {
}
