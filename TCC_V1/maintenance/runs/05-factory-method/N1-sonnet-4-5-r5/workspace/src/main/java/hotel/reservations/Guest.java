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

    public GuestType type() {
        if (cpf != null) {
            return GuestType.INDIVIDUAL;
        } else if (companyCnpj != null) {
            return GuestType.COMPANY;
        } else {
            return GuestType.FOREIGN;
        }
    }

    public enum GuestType {
        INDIVIDUAL, COMPANY, FOREIGN
    }
}
