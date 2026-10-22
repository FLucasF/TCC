package pousada.identity;

import pousada.document.IssuedDocument;
import pousada.document.Stay;
import pousada.document.Wording;
import pousada.domain.Money;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CnpjIdentity(String cnpj, String companyName) implements Identity {
    private static final String TITLE = "Fatura de hospedagem";
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public static final IdentityKind KIND = new IdentityKind() {
        @Override
        public String name() {
            return "CNPJ";
        }

        @Override
        public List<String> fieldNames() {
            return List.of("companyCnpj", "companyName");
        }

        @Override
        public Identity create(Map<String, String> fields) {
            return new CnpjIdentity(Fields.required(fields, "companyCnpj"), Fields.required(fields, "companyName"));
        }
    };

    @Override
    public IdentityKind kind() {
        return KIND;
    }

    @Override
    public Map<String, String> fields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("companyCnpj", cnpj);
        fields.put("companyName", companyName);
        return fields;
    }

    @Override
    public IssuedDocument issue(Stay stay) {
        Wording wording = Wording.PORTUGUESE;
        List<String> identityLines = List.of(
                "Empresa: " + companyName + " (CNPJ " + cnpj + ")",
                "Hóspede: " + stay.guestName());
        BigDecimal iss = Money.round(stay.total().multiply(ISS_RATE));
        List<String> footer = List.of("ISS retido (5%): " + wording.money(iss));
        return new IssuedDocument(TITLE, wording.render(TITLE, stay, identityLines, footer));
    }
}
