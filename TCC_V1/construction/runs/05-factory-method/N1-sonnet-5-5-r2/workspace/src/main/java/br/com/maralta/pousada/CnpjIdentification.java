package br.com.maralta.pousada;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CnpjIdentification(String cnpj, String companyName) implements Identification {
    static final Identifications.Kind KIND = new Identifications.Kind("CNPJ", List.of("companyCnpj", "companyName"),
            f -> new CnpjIdentification(f.get("companyCnpj"), f.get("companyName")));

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public Map<String, String> fields() {
        return Map.of("companyCnpj", cnpj, "companyName", companyName);
    }

    @Override
    public Language language() {
        return Language.PT;
    }

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    public List<String> partyLines(String guestName) {
        return List.of("Empresa: " + companyName + " (CNPJ " + cnpj + ")", "Hóspede: " + guestName);
    }

    @Override
    public List<String> footerLines(Document document) {
        BigDecimal iss = Money.round(document.total().multiply(ISS_RATE));
        return List.of("ISS retido (5%): " + language().money(iss));
    }
}
