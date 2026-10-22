package hotel.reservations;

/** A guest making a reservation: an individual, a company, or a foreigner. */
public sealed interface Guest permits IndividualGuest, CorporateGuest, ForeignGuest {
    String name();
    String email();
}
