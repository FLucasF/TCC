package br.com.marealta.identity;

import br.com.marealta.domain.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

final class CnpjIdentity implements IdentityKind {
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public String code() { return "CNPJ"; }

    public List<String> fieldNames() { return List.of("companyCnpj", "companyName"); }

    public String documentTitle() { return "Fatura de hospedagem"; }

    public Language language() { return Language.PT_BR; }

    public List<String> identityLines(String guestName, Map<String, String> values) {
        return List.of(
                "Empresa: " + values.get("companyName") + " (CNPJ " + values.get("companyCnpj") + ")",
                "Hóspede: " + guestName);
    }

    @Override
    public List<String> closingLines(BigDecimal total) {
        return List.of("ISS retido (5%): " + language().money(Money.round(total.multiply(ISS_RATE))));
    }
}
