package pousada.document;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pousada.ApiException;
import pousada.domain.Identity;

/** Os tipos de documento que existem. Um tipo novo entra só na lista abaixo. */
public final class DocumentTypes {
    private static final List<DocumentType> ALL = List.of(
            new CpfReceipt(),
            new CompanyInvoice(),
            new PassportReceipt());

    private DocumentTypes() {
    }

    /** Todos os campos de identificação de todos os tipos, na ordem de declaração. */
    public static Set<String> allFields() {
        Set<String> fields = new LinkedHashSet<>();
        ALL.forEach(type -> fields.addAll(type.fields()));
        return fields;
    }

    public static DocumentType byCode(String code) {
        return ALL.stream().filter(type -> type.code().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Tipo de documento desconhecido: " + code));
    }

    /**
     * Descobre como o hóspede se identifica. {@code given} traz cada campo de identificação
     * (null ou vazio quando não veio); tem que haver exatamente um tipo, com todos os seus campos.
     */
    public static Identity resolve(Map<String, String> given) {
        List<DocumentType> used = ALL.stream()
                .filter(type -> type.fields().stream().anyMatch(field -> present(given.get(field))))
                .toList();
        if (used.size() != 1) {
            throw ApiException.invalid("Informe a identificação de um único jeito: guestCpf, "
                    + "companyCnpj com companyName, ou guestPassport com guestCountry");
        }
        DocumentType type = used.get(0);
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : type.fields()) {
            if (!present(given.get(field))) {
                throw ApiException.invalid("Campo obrigatório ausente: " + field);
            }
            values.put(field, given.get(field).trim());
        }
        return new Identity(type.code(), values);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
