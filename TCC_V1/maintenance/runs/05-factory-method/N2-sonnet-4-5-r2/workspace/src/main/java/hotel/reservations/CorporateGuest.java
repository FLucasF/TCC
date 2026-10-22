package hotel.reservations;

/** A company booking for an employee, identified by CNPJ. */
public record CorporateGuest(String name, String email, String cnpj, String companyName) implements Guest {
}
