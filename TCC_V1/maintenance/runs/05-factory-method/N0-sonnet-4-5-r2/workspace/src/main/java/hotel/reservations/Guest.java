package hotel.reservations;

public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String passport,
        String country) {

    /** Creates a guest identified by CPF (Brazilian individual). */
    public static Guest withCpf(String name, String email, String cpf) {
        return new Guest(name, email, cpf, null, null, null, null);
    }

    /** Creates a guest identified by company CNPJ (Brazilian company). */
    public static Guest withCompany(String name, String email, String cnpj, String companyName) {
        return new Guest(name, email, null, cnpj, companyName, null, null);
    }

    /** Creates a guest identified by passport (foreigner). */
    public static Guest withPassport(String name, String email, String passport, String country) {
        return new Guest(name, email, null, null, null, passport, country);
    }

    /** Returns true if this is a company reservation (identified by CNPJ). */
    public boolean isCompany() {
        return companyCnpj != null;
    }

    /** Returns true if this is a foreigner reservation (identified by passport). */
    public boolean isForeigner() {
        return passport != null;
    }
}
