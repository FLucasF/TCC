package pousada.identity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import pousada.Money;

public record CnpjIdentity(String cnpj, String companyName) implements GuestIdentity {
    static final String TITLE = "Fatura de hospedagem";
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    static final IdentityKind KIND = new IdentityKind(List.of("companyCnpj", "companyName"),
            values -> new CnpjIdentity(values.get("companyCnpj"), values.get("companyName")));

    @Override
    public Map<String, String> fields() {
        return Map.of("companyCnpj", cnpj, "companyName", companyName);
    }

    @Override
    public String documentTitle() {
        return TITLE;
    }

    @Override
    public String renderDocument(DocumentData data) {
        BigDecimal iss = Money.round(data.quote().total().multiply(ISS_RATE));
        return DocumentLayout.render(DocumentLayout.PORTUGUESE, TITLE, data,
                List.of("Empresa: " + companyName + " (CNPJ " + cnpj + ")",
                        "Hóspede: " + data.guestName()),
                List.of("ISS retido (5%): " + DocumentLayout.PORTUGUESE.money().apply(iss)));
    }
}
