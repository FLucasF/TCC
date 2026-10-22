package hotel.reservations;

public record CompanyGuest(String name, String email, String cnpj, String companyName) implements Guest {
}
