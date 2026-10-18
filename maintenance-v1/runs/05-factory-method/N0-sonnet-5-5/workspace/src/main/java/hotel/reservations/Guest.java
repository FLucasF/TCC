package hotel.reservations;

public record Guest(String name, String email, GuestIdentity identity) {

    public Guest(String name, String email, String cpf) {
        this(name, email, new GuestIdentity.Cpf(cpf));
    }

    /** Null when the guest is not identified by CPF; the same goes for the accessors below. */
    public String cpf() {
        return identity instanceof GuestIdentity.Cpf id ? id.cpf() : null;
    }

    public String companyCnpj() {
        return identity instanceof GuestIdentity.Company id ? id.cnpj() : null;
    }

    public String companyName() {
        return identity instanceof GuestIdentity.Company id ? id.companyName() : null;
    }

    public String passport() {
        return identity instanceof GuestIdentity.Passport id ? id.number() : null;
    }

    public String country() {
        return identity instanceof GuestIdentity.Passport id ? id.country() : null;
    }
}
