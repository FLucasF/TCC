package br.com.marealta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro dos tipos de identificação aceitos. Um tipo novo entra só na lista do construtor. */
public class IdentityTypes {
    private final List<IdentityType> types;

    public IdentityTypes(List<IdentityType> types) {
        this.types = List.copyOf(types);
    }

    public static IdentityTypes standard() {
        return new IdentityTypes(List.of(new CpfIdentity(), new CnpjIdentity(), new PassportIdentity()));
    }

    /** Todos os campos de identificação que a API mostra, na ordem dos tipos. */
    public List<String> allFields() {
        return types.stream().flatMap(t -> t.fields().stream()).toList();
    }

    public IdentityType byKind(String kind) {
        return types.stream().filter(t -> t.kind().equals(kind)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Tipo de identificação desconhecido: " + kind));
    }

    /** Exatamente um tipo pode aparecer no corpo, com todos os seus campos preenchidos. */
    public GuestIdentity parse(Map<String, Object> body) {
        List<IdentityType> used = new ArrayList<>();
        for (IdentityType type : types) {
            if (type.fields().stream().anyMatch(f -> body.get(f) != null)) used.add(type);
        }
        if (used.size() != 1) {
            throw AppException.invalid("Informe a identificação de um só jeito: " + describeOptions());
        }
        IdentityType type = used.get(0);
        Map<String, String> data = new LinkedHashMap<>();
        for (String field : type.fields()) {
            if (!(body.get(field) instanceof String text) || text.isBlank()) {
                throw AppException.invalid("Campo obrigatório ausente ou inválido: " + field);
            }
            data.put(field, text.trim());
        }
        return new GuestIdentity(type, data);
    }

    private String describeOptions() {
        return String.join(" ou ", types.stream().map(t -> String.join(" + ", t.fields())).toList());
    }
}
