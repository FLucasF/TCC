package br.com.marealta.domain;

/** Um jeito de o hóspede se identificar. */
public interface IdentityKind {
    /** Algum dos campos deste jeito veio preenchido. */
    boolean isPresent(IdentityFields fields);

    /** Monta a identidade; falha com INVALID_REQUEST se faltar algum campo deste jeito. */
    GuestIdentity create(IdentityFields fields);

    static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw DomainException.invalid("Campo obrigatório: " + fieldName);
        }
        return value;
    }
}
