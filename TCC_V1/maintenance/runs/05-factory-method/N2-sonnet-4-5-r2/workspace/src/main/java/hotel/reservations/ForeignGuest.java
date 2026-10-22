package hotel.reservations;

/** A foreign guest identified by passport. */
public record ForeignGuest(String name, String email, String passport, String country) implements Guest {
}
