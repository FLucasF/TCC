package pousada.identity;

import pousada.domain.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CnpjIdentity(String cnpj, String companyName) implements GuestIdentity {
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public IdentityKind kind() {
        return IdentityKind.CNPJ;
    }

    @Override
    public Map<String, String> fields() {
        return Map.of("companyCnpj", cnpj, "companyName", companyName);
    }

    @Override
    public DocumentLanguage language() {
        return DocumentLanguage.PORTUGUESE;
    }

    @Override
    public String documentTitle() {
        return "Fatura de hospedagem";
    }

    @Override
    public List<String> partyLines(String guestName) {
        return List.of("Empresa: " + companyName + " (CNPJ " + cnpj + ")", "Hóspede: " + guestName);
    }

    @Override
    public List<String> footerLines(BigDecimal total) {
        BigDecimal iss = Money.round(total.multiply(ISS_RATE));
        return List.of("ISS retido (5%): " + language().money(iss));
    }
}
