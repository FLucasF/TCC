package br.com.marealta.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registro dos jeitos de se identificar. Um novo jeito entra aqui, e só aqui. */
public final class IdentityKinds {
    private static final List<IdentityKind> KINDS = List.of(
            CpfIdentity.KIND,
            CnpjIdentity.KIND,
            PassportIdentity.KIND);

    private IdentityKinds() {
    }

    /** Todos os campos de identificação, na ordem em que aparecem na API. */
    public static List<String> allFields() {
        return KINDS.stream().flatMap(k -> k.fields().stream()).toList();
    }

    /**
     * Monta a identificação a partir dos campos informados (nulos ou ausentes não contam).
     * Vazio se não for exatamente um jeito completo.
     */
    public static Optional<Identity> from(Map<String, String> given) {
        List<IdentityKind> used = KINDS.stream()
                .filter(k -> k.fields().stream().anyMatch(f -> isPresent(given, f)))
                .toList();
        if (used.size() != 1) {
            return Optional.empty();
        }
        IdentityKind kind = used.get(0);
        if (!kind.fields().stream().allMatch(f -> isPresent(given, f))) {
            return Optional.empty();
        }
        return Optional.of(kind.factory().apply(given));
    }

    private static boolean isPresent(Map<String, String> given, String field) {
        String value = given.get(field);
        return value != null && !value.isBlank();
    }
}
