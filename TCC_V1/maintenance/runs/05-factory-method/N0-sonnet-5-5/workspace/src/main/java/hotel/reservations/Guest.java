package hotel.reservations;

/** The guest staying, identified by CPF, or by the company paying (CNPJ), or by passport; the rest are null. */
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

    public boolean isCompany() {
        return companyCnpj != null;
    }

    public boolean isForeigner() {
        return passport != null;
    }
}
