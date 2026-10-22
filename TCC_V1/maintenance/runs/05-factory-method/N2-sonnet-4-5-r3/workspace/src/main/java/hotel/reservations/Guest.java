package hotel.reservations;

public sealed interface Guest permits PersonGuest, CompanyGuest, ForeignGuest {
    String name();
    String email();

    static Guest withCpf(String name, String email, String cpf) {
        return new PersonGuest(name, email, cpf);
    }

    static Guest withCompany(String name, String email, String cnpj, String companyName) {
        return new CompanyGuest(name, email, cnpj, companyName);
    }

    static Guest withPassport(String name, String email, String passport, String country) {
        return new ForeignGuest(name, email, passport, country);
    }
}
