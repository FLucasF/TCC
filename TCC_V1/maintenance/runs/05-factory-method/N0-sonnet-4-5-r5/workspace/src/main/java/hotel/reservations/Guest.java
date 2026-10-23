package hotel.reservations;

public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String passport,
        String country) {

    public Guest(String name, String email, String cpf) {
        this(name, email, cpf, null, null, null, null);
    }

    public Guest(String name, String email, String companyCnpj, String companyName) {
        this(name, email, null, companyCnpj, companyName, null, null);
    }

    public static Guest withPassport(String name, String email, String passport, String country) {
        return new Guest(name, email, null, null, null, passport, country);
    }

    public GuestType type() {
        if (cpf != null) {
            return GuestType.INDIVIDUAL;
        } else if (companyCnpj != null) {
            return GuestType.COMPANY;
        } else {
            return GuestType.FOREIGN;
        }
    }
}
