package br.com.mare.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Único lugar que conhece todos os jeitos de se identificar. Um novo tipo entra só aqui. */
public final class IdentityKinds {
    private static final List<IdentityKind> ALL = List.of(
            CpfIdentity.KIND,
            CompanyIdentity.KIND,
            PassportIdentity.KIND);

    private static final ObjectMapper JSON = new ObjectMapper();

    private IdentityKinds() {
    }

    /** Todos os campos de identificação que a API expõe, na ordem de cadastro. */
    public static List<String> allFieldNames() {
        List<String> names = new ArrayList<>();
        ALL.forEach(k -> names.addAll(k.fieldNames()));
        return names;
    }

    /** Lê a identificação do corpo: exatamente um tipo, com todos os seus campos preenchidos. */
    public static GuestIdentity fromBody(Map<String, Object> body) {
        List<IdentityKind> present = ALL.stream()
                .filter(k -> k.fieldNames().stream().anyMatch(f -> body.get(f) != null))
                .toList();
        if (present.size() != 1) {
            throw invalid();
        }
        IdentityKind kind = present.get(0);
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : kind.fieldNames()) {
            if (!(body.get(field) instanceof String s) || s.isBlank()) {
                throw invalid();
            }
            values.put(field, s.trim());
        }
        return kind.create(values);
    }

    public static String kindName(GuestIdentity identity) {
        return identity.kind().name();
    }

    public static String serialize(GuestIdentity identity) {
        try {
            return JSON.writeValueAsString(identity.values());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static GuestIdentity restore(String kindName, String data) {
        try {
            Map<String, String> values = JSON.readValue(data, Map.class);
            return ALL.stream().filter(k -> k.name().equals(kindName)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Tipo de identificação desconhecido: " + kindName))
                    .create(values);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static PousadaException invalid() {
        return new PousadaException(ErrorCode.INVALID_REQUEST,
                "Informe exatamente uma identificação: guestCpf, companyCnpj com companyName, "
                        + "ou guestPassport com guestCountry");
    }
}
