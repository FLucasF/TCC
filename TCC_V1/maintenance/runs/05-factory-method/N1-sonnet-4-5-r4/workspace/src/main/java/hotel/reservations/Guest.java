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

    public Guest(String name, String email, String passport, String country, boolean isPassport) {
        this(name, email, null, null, null, passport, country);
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
