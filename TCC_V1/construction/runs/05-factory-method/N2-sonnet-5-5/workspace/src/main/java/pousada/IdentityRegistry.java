package pousada;

import java.util.List;

/** Único lugar que conhece os tipos de identificação aceitos. */
public class IdentityRegistry {
    private final List<IdentityType> types = List.of(CpfIdentity.TYPE, CnpjIdentity.TYPE, PassportIdentity.TYPE);

    /** Todos os campos de identificação que aparecem no JSON, na ordem de cadastro. */
    public List<String> allKeys() {
        return types.stream().flatMap(t -> t.keys().stream()).toList();
    }

    /** Exige exatamente um tipo de identificação, com todos os campos dele. */
    public GuestIdentity parse(RequestBody body) {
        List<IdentityType> used = types.stream()
                .filter(t -> t.keys().stream().anyMatch(body::has))
                .toList();
        if (used.size() != 1) {
            throw ApiException.invalidRequest(
                    "Informe a identificação de um único jeito: " + describeOptions());
        }
        return used.get(0).parse(body);
    }

    private String describeOptions() {
        return String.join(" | ", types.stream().map(t -> String.join("+", t.keys())).toList());
    }
}
