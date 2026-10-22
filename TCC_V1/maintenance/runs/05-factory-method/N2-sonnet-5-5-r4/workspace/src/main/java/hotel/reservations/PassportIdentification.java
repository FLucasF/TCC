package hotel.reservations;

/** A foreign guest, identified by passport number and country. */
public record PassportIdentification(String passport, String country) implements Identification {
}
