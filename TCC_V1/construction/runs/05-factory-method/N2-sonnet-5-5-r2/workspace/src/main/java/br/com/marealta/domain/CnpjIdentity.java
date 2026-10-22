package br.com.marealta.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CnpjIdentity extends PortugueseIdentity {
    public static final IdentityKind KIND = new IdentityKind(List.of("companyCnpj", "companyName"),
            f -> new CnpjIdentity(f.get("companyCnpj"), f.get("companyName")));

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private final String cnpj;
    private final String companyName;

    private CnpjIdentity(String cnpj, String companyName) {
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    @Override
    public Map<String, String> fields() {
        return Map.of("companyCnpj", cnpj, "companyName", companyName);
    }

    @Override
    protected String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected List<String> partyLines(ReceiptData d) {
        return List.of("Empresa: " + companyName + " (CNPJ " + cnpj + ")", "Hóspede: " + d.guestName());
    }

    @Override
    protected List<String> footerLines(ReceiptData d) {
        return List.of("ISS retido (5%): " + money(Money.round(d.total().multiply(ISS_RATE))));
    }
}
