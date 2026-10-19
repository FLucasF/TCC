package br.com.mealta.pousada;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Empresa com CNPJ: recebe a fatura de hospedagem, com ISS retido. */
public record CnpjIdentity(String cnpj, String companyName) implements Identity {
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public static final IdentityType TYPE = new IdentityType() {
        public List<String> fieldNames() { return List.of("companyCnpj", "companyName"); }
        public Identity create(Map<String, String> v) {
            return new CnpjIdentity(v.get("companyCnpj"), v.get("companyName"));
        }
    };

    public Map<String, String> fields() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("companyCnpj", cnpj);
        f.put("companyName", companyName);
        return f;
    }

    public String title() { return "Fatura de hospedagem"; }

    public String render(DocumentData d) {
        List<String> lines = new ArrayList<>();
        lines.add("Fatura de hospedagem nº " + d.reservationId());
        lines.add("Empresa: " + companyName + " (CNPJ " + cnpj + ")");
        lines.addAll(PtBrLayout.details(d, true));
        lines.add("ISS retido (5%): " + Money.brl(Money.round(d.total().multiply(ISS_RATE))));
        return String.join("\n", lines);
    }
}
