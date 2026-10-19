package pousada.domain.identification;

import pousada.domain.DomainException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro das formas de identificação: é o único lugar a mexer para aceitar uma nova. */
public final class IdentificationKinds {

    private static final List<IdentificationKind> KINDS = List.of(
            new CpfIdentification.Kind(),
            new CnpjIdentification.Kind(),
            new PassportIdentification.Kind());

    private IdentificationKinds() {
    }

    /** Todos os campos de identificação, na ordem em que aparecem no JSON. */
    public static List<String> allFieldNames() {
        List<String> names = new ArrayList<>();
        KINDS.forEach(kind -> names.addAll(kind.fieldNames()));
        return names;
    }

    /** {@code given} traz os campos presentes no corpo (valores nulos já descartados). */
    public static Identification parse(Map<String, String> given) {
        List<IdentificationKind> used = KINDS.stream()
                .filter(kind -> kind.fieldNames().stream().anyMatch(given::containsKey))
                .toList();
        if (used.size() != 1) {
            throw DomainException.invalidRequest(
                    "Informe a identificação de um único jeito: " + describeKinds());
        }
        IdentificationKind kind = used.get(0);
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : kind.fieldNames()) {
            String value = given.get(name);
            if (value == null || value.isBlank()) {
                throw DomainException.invalidRequest("Campo obrigatório ausente: " + name);
            }
            values.put(name, value.trim());
        }
        return kind.create(values);
    }

    /** Mapa com todos os campos de identificação, null nos que não se aplicam. */
    public static Map<String, String> withNulls(Identification identification) {
        Map<String, String> all = new LinkedHashMap<>();
        for (String name : allFieldNames()) {
            all.put(name, identification.fields().get(name));
        }
        return all;
    }

    private static String describeKinds() {
        return String.join(" | ", KINDS.stream().map(kind -> String.join("+", kind.fieldNames())).toList());
    }
}
