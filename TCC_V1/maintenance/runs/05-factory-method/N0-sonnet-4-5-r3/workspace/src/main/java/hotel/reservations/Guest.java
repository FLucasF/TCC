package hotel.reservations;

public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String passport,
        String country) {

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
