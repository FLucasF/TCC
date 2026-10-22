package pousada;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro dos jeitos de identificação. Um novo jeito entra aqui, com o seu documento. */
final class IdentificationTypes {
    private static final List<IdentificationType> ALL = List.of(
            new IdentificationType("CPF", List.of("guestCpf"), new CpfReceipt()),
            new IdentificationType("CNPJ", List.of("companyCnpj", "companyName"), new LodgingInvoice()),
            new IdentificationType("PASSPORT", List.of("guestPassport", "guestCountry"), new BookingReceipt()));

    private IdentificationTypes() {
    }

    static List<String> allFields() {
        List<String> fields = new ArrayList<>();
        ALL.forEach(t -> fields.addAll(t.fields()));
        return fields;
    }

    static IdentificationType byCode(String code) {
        return ALL.stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
    }

    static Identification fromRequest(JsonNode body) {
        List<IdentificationType> used = new ArrayList<>();
        for (IdentificationType type : ALL) {
            if (type.fields().stream().anyMatch(f -> isPresent(body, f))) {
                used.add(type);
            }
        }
        if (used.size() != 1) {
            throw ApiException.invalid("Informe a identificação de um só jeito: guestCpf, companyCnpj com "
                    + "companyName, ou guestPassport com guestCountry.");
        }
        IdentificationType type = used.get(0);
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : type.fields()) {
            JsonNode node = body.get(field);
            if (node == null || !node.isTextual() || node.asText().isBlank()) {
                throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
            }
            values.put(field, node.asText().trim());
        }
        return new Identification(type, values);
    }

    private static boolean isPresent(JsonNode body, String field) {
        JsonNode node = body.get(field);
        return node != null && !node.isNull();
    }
}
