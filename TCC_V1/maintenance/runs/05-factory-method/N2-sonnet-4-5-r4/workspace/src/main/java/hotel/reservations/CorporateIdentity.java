package hotel.reservations;

public record CorporateIdentity(String cnpj, String companyName) implements GuestIdentity {
}
