package hotel.reservations;

public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry) {

    public static Guest withCpf(String name, String email, String cpf) {
        return new Guest(name, email, cpf, null, null, null, null);
    }

    public static Guest withCompany(String name, String email, String companyCnpj, String companyName) {
        return new Guest(name, email, null, companyCnpj, companyName, null, null);
    }

    public static Guest withPassport(String name, String email, String guestPassport, String guestCountry) {
        return new Guest(name, email, null, null, null, guestPassport, guestCountry);
    }
}
