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

    public boolean isCpf() {
        return cpf != null;
    }

    public boolean isCompany() {
        return companyCnpj != null;
    }

    public boolean isPassport() {
        return passport != null;
    }
}
