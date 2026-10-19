package br.com.mealta.pousada;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro dos jeitos de se identificar. Um novo tipo entra só na lista TYPES. */
public final class IdentityTypes {
    private static final List<IdentityType> TYPES =
            List.of(CpfIdentity.TYPE, CnpjIdentity.TYPE, PassportIdentity.TYPE);

    private IdentityTypes() {}

    /** Todos os campos de identificação, na ordem do registro. */
    public static List<String> allFieldNames() {
        List<String> names = new ArrayList<>();
        TYPES.forEach(t -> names.addAll(t.fieldNames()));
        return names;
    }

    /** Campos de identificação de uma reserva: os que não se aplicam vêm null. */
    public static Map<String, String> view(Identity identity) {
        Map<String, String> view = new LinkedHashMap<>();
        allFieldNames().forEach(n -> view.put(n, null));
        view.putAll(identity.fields());
        return view;
    }

    /** Exige exatamente um tipo, com todos os seus campos preenchidos. */
    public static Identity parse(Map<String, Object> body) {
        Identity found = null;
        for (IdentityType type : TYPES) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String name : type.fieldNames()) {
                Object raw = body.get(name);
                if (raw == null) continue;
                if (!(raw instanceof String s)) {
                    throw DomainException.invalid("Campo " + name + " deve ser texto");
                }
                if (!s.isBlank()) values.put(name, s.trim());
            }
            if (values.isEmpty()) continue;
            if (found != null) {
                throw DomainException.invalid("Informe apenas um jeito de identificação");
            }
            if (values.size() != type.fieldNames().size()) {
                throw DomainException.invalid("Identificação incompleta: informe " + type.fieldNames());
            }
            found = type.create(values);
        }
        if (found == null) {
            throw DomainException.invalid("Informe CPF, CNPJ com razão social ou passaporte com país");
        }
        return found;
    }
}
