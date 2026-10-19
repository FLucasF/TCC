package br.com.marealta;

/** How the guest identifies themselves; decides which document they receive. */
sealed interface Identification {
    record Cpf(String cpf) implements Identification {}

    record Cnpj(String cnpj, String companyName) implements Identification {}

    record Passport(String passport, String country) implements Identification {}
}
