package pousada.identity;

import pousada.BusinessException;
import pousada.ErrorCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Todas as formas de identificação aceitas. Uma nova forma entra aqui, com seus campos e sua fábrica. */
public enum IdentityKind {
    CPF(List.of("guestCpf"),
            v -> new CpfIdentity(v.get("guestCpf"))),
    CNPJ(List.of("companyCnpj", "companyName"),
            v -> new CnpjIdentity(v.get("companyCnpj"), v.get("companyName"))),
    PASSPORT(List.of("guestPassport", "guestCountry"),
            v -> new PassportIdentity(v.get("guestPassport"), v.get("guestCountry")));

    private final List<String> fieldNames;
    private final Function<Map<String, String>, GuestIdentity> factory;

    IdentityKind(List<String> fieldNames, Function<Map<String, String>, GuestIdentity> factory) {
        this.fieldNames = fieldNames;
        this.factory = factory;
    }

    public GuestIdentity create(Map<String, String> values) {
        return factory.apply(values);
    }

    /** Nomes de todos os campos de identificação, na ordem em que aparecem na API. */
    public static List<String> allFieldNames() {
        List<String> names = new ArrayList<>();
        for (IdentityKind kind : values()) {
            names.addAll(kind.fieldNames);
        }
        return names;
    }

    /** Todos os campos de identificação; os que a identidade não preenche ficam null. */
    public static Map<String, String> fieldsOf(GuestIdentity identity) {
        Map<String, String> all = new LinkedHashMap<>();
        for (String name : allFieldNames()) {
            all.put(name, identity.fields().get(name));
        }
        return all;
    }

    /**
     * Descobre a forma de identificação a partir dos valores informados (campo ausente, nulo ou em branco
     * conta como não informado). Tem que haver uma, e só uma, forma, com todos os seus campos.
     */
    public static GuestIdentity fromValues(Map<String, String> informed) {
        List<IdentityKind> used = new ArrayList<>();
        for (IdentityKind kind : values()) {
            if (kind.fieldNames.stream().anyMatch(n -> informed.get(n) != null && !informed.get(n).isBlank())) {
                used.add(kind);
            }
        }
        if (used.size() != 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Informe uma, e só uma, forma de identificação: guestCpf, companyCnpj + companyName "
                            + "ou guestPassport + guestCountry");
        }
        IdentityKind kind = used.get(0);
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : kind.fieldNames) {
            String value = informed.get(name);
            if (value == null || value.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Campo obrigatório: " + name);
            }
            values.put(name, value);
        }
        return kind.create(values);
    }
}
