package hotel.reservations;

/** A corporate booking, identified by CNPJ. The guest's name is the person staying, not the company. */
public record CorporateGuest(String name, String email, String companyCnpj, String companyName) implements Guest {
}
