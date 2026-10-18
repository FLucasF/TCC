package hotel.reservations;

/** A foreign guest, identified by passport number and country. */
public record ForeignGuest(String name, String email, String passportNumber, String country) implements Guest {
}
