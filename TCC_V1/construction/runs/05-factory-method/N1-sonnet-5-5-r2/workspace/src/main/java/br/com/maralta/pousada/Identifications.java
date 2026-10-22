package br.com.maralta.pousada;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Registro das formas de identificação aceitas. */
public final class Identifications {
    /** Uma forma de identificação: seus campos (todos obrigatórios) e como montá-la. */
    public record Kind(String name, List<String> fieldNames, Function<Map<String, String>, Identification> factory) {
    }

    public static final List<Kind> KINDS = List.of(CpfIdentification.KIND, CnpjIdentification.KIND,
            PassportIdentification.KIND);

    private Identifications() {
    }

    /** Nomes de todos os campos de identificação que a API expõe. */
    public static List<String> allFieldNames() {
        return KINDS.stream().flatMap(k -> k.fieldNames().stream()).toList();
    }

    /** Todos os campos, com null nos que não se aplicam. */
    public static Map<String, String> allFields(Identification identification) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : allFieldNames()) {
            result.put(name, identification.fields().get(name));
        }
        return result;
    }

    /** Monta a identificação a partir dos campos informados; exige exatamente uma forma, completa. */
    public static Identification from(Map<String, String> given) {
        List<Kind> used = new ArrayList<>();
        for (Kind kind : KINDS) {
            if (kind.fieldNames().stream().anyMatch(given::containsKey)) {
                used.add(kind);
            }
        }
        if (used.size() != 1) {
            throw ApiException.invalidRequest("Informe a identificação de um só jeito: "
                    + KINDS.stream().map(k -> String.join(" + ", k.fieldNames())).toList());
        }
        Kind kind = used.get(0);
        for (String name : kind.fieldNames()) {
            String value = given.get(name);
            if (value == null || value.isBlank()) {
                throw ApiException.invalidRequest("Campo obrigatório: " + name);
            }
        }
        return kind.factory().apply(given);
    }
}
