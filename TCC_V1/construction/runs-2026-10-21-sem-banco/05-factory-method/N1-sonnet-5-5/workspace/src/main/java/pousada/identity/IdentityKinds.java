package pousada.identity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import pousada.ApiException;

/** Registro das formas de identificação. Uma nova forma entra só na lista ALL. */
public final class IdentityKinds {
    private static final List<IdentityKind> ALL = List.of(
            CpfIdentity.KIND,
            CnpjIdentity.KIND,
            PassportIdentity.KIND);

    private IdentityKinds() {
    }

    /** Todos os campos de identificação que a API expõe, na ordem de registro. */
    public static List<String> fieldNames() {
        return ALL.stream().flatMap(k -> k.fields().stream()).toList();
    }

    /** Campos de identificação para JSON, com null nos que não se aplicam. */
    public static Map<String, String> toJsonFields(GuestIdentity identity) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, String> own = identity.fields();
        for (String name : fieldNames()) {
            result.put(name, own.get(name));
        }
        return result;
    }

    /** Lê do corpo da requisição exatamente uma forma de identificação completa. */
    public static GuestIdentity parse(JsonNode body) {
        List<GuestIdentity> found = new ArrayList<>();
        for (IdentityKind kind : ALL) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String field : kind.fields()) {
                String value = textOrNull(body.get(field), field);
                if (value != null) {
                    values.put(field, value);
                }
            }
            if (values.isEmpty()) {
                continue;
            }
            if (values.size() != kind.fields().size()) {
                throw ApiException.invalid("Identificação incompleta: informe " + kind.fields() + ".");
            }
            found.add(kind.create(values));
        }
        if (found.size() != 1) {
            throw ApiException.invalid("Informe exatamente uma identificação: " + ALL.stream().map(IdentityKind::fields).toList() + ".");
        }
        return found.get(0);
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw ApiException.invalid("O campo " + field + " deve ser texto.");
        }
        String text = node.asText().trim();
        return text.isEmpty() ? null : text;
    }
}
