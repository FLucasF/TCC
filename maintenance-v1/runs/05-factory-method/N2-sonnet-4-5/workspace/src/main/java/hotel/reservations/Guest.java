package hotel.reservations;

/** The person staying at the inn. The inn accepts individuals, corporations, and foreign guests. */
public sealed interface Guest permits IndividualGuest, CorporateGuest, ForeignGuest {
    String name();
    String email();
}
