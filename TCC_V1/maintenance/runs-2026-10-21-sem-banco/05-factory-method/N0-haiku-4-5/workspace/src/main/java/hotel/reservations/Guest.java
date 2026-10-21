package hotel.reservations;

public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry) {

    public Guest(String name, String email, String cpf) {
        this(name, email, cpf, null, null, null, null);
    }
}

