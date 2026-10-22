package pousada.identity;

import pousada.domain.PousadaException;

import java.util.List;
import java.util.Map;

/** Lista das formas de identificação aceitas. Uma nova forma entra aqui, e só aqui. */
public final class IdentityKinds {
    private static final List<IdentityKind> ALL = List.of(
            CpfIdentity.KIND,
            CnpjIdentity.KIND,
            PassportIdentity.KIND);

    private IdentityKinds() {
    }

    /** Todos os campos de identificação que a reserva expõe, na ordem de declaração. */
    public static List<String> allFieldNames() {
        return ALL.stream().flatMap(k -> k.fieldNames().stream()).toList();
    }

    /** Escolhe a única forma de identificação cujos campos vieram; qualquer outra combinação é inválida. */
    public static Identity fromRequest(Map<String, String> given) {
        List<IdentityKind> used = ALL.stream()
                .filter(k -> k.fieldNames().stream().anyMatch(given::containsKey))
                .toList();
        if (used.size() != 1) {
            throw PousadaException.invalid("Informe a identificação de um só jeito: "
                    + String.join(" ou ", ALL.stream().map(k -> String.join("+", k.fieldNames())).toList()));
        }
        return used.get(0).create(given);
    }

    public static Identity restore(String kindName, Map<String, String> fields) {
        return ALL.stream().filter(k -> k.name().equals(kindName)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Identificação desconhecida: " + kindName))
                .create(fields);
    }
}
