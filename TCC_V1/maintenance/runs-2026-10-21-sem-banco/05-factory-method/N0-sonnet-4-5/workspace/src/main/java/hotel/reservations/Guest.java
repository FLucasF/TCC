package hotel.reservations;

public record Guest(
        String name,
        String email,
        String cpf,
        String companyCnpj,
        String companyName,
        String passport,
        String country) {

    public static Guest withCpf(String name, String email, String cpf) {
        return new Guest(name, email, cpf, null, null, null, null);
    }

    public static Guest withCompany(String name, String email, String cnpj, String companyName) {
        return new Guest(name, email, null, cnpj, companyName, null, null);
    }

    public static Guest withPassport(String name, String email, String passport, String country) {
        return new Guest(name, email, null, null, null, passport, country);
    }

    public GuestType type() {
        if (cpf != null) return GuestType.INDIVIDUAL;
        if (companyCnpj != null) return GuestType.COMPANY;
        return GuestType.FOREIGNER;
    }

    public enum GuestType {
        INDIVIDUAL, COMPANY, FOREIGNER
    }
}
