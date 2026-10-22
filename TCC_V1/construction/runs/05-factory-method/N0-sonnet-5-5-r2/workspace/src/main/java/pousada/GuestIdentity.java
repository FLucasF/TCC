package pousada;

/**
 * How the guest identifies themselves. Each variant decides which document it receives
 * (see {@link Documents}); adding a variant forces the compiler to flag every place to update.
 */
public sealed interface GuestIdentity {
    record Individual(String cpf) implements GuestIdentity {}

    record Company(String cnpj, String companyName) implements GuestIdentity {}

    record Foreigner(String passport, String country) implements GuestIdentity {}

    default String cpf() { return this instanceof Individual i ? i.cpf() : null; }

    default String cnpj() { return this instanceof Company c ? c.cnpj() : null; }

    default String companyName() { return this instanceof Company c ? c.companyName() : null; }

    default String passport() { return this instanceof Foreigner f ? f.passport() : null; }

    default String country() { return this instanceof Foreigner f ? f.country() : null; }
}
