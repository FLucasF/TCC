package br.com.marealta.identity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Catálogo dos jeitos de identificação. Um novo jeito entra apenas na lista abaixo. */
public final class IdentityKinds {
    private static final List<IdentityKind> ALL = List.of(
            new CpfIdentity(),
            new CnpjIdentity(),
            new PassportIdentity());

    private IdentityKinds() {}

    /** Todos os campos de identificação que a requisição pode trazer, na ordem do catálogo. */
    public static Set<String> allFieldNames() {
        Set<String> names = new LinkedHashSet<>();
        ALL.forEach(kind -> names.addAll(kind.fieldNames()));
        return names;
    }

    /** Acha o jeito cujos campos são exatamente os informados. */
    public static Optional<IdentityKind> resolve(Set<String> givenFields) {
        return ALL.stream().filter(kind -> Set.copyOf(kind.fieldNames()).equals(givenFields)).findFirst();
    }

    public static IdentityKind byCode(String code) {
        return ALL.stream().filter(kind -> kind.code().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Identificação desconhecida: " + code));
    }

    /** Os campos de identificação de um hóspede, com null nos que não se aplicam. */
    public static Map<String, String> describe(GuestIdentity identity) {
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        allFieldNames().forEach(name -> fields.put(name, identity.values().get(name)));
        return fields;
    }
}
