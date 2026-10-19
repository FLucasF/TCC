package br.com.mealta.pousada;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pessoa com CPF: recebe o comprovante de reserva. */
public record CpfIdentity(String cpf) implements Identity {
    public static final IdentityType TYPE = new IdentityType() {
        public List<String> fieldNames() { return List.of("guestCpf"); }
        public Identity create(Map<String, String> v) { return new CpfIdentity(v.get("guestCpf")); }
    };

    public Map<String, String> fields() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("guestCpf", cpf);
        return f;
    }

    public String title() { return "Comprovante de reserva"; }

    public String render(DocumentData d) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add("Comprovante de reserva nº " + d.reservationId());
        lines.add("Hóspede: " + d.guestName() + " (CPF " + cpf + ")");
        lines.addAll(PtBrLayout.details(d, false));
        return String.join("\n", lines);
    }
}
