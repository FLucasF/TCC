package br.com.marealta.domain;

import java.util.List;
import java.util.Map;

public final class CpfIdentity extends PortugueseIdentity {
    public static final IdentityKind KIND = new IdentityKind(List.of("guestCpf"),
            f -> new CpfIdentity(f.get("guestCpf")));

    private final String cpf;

    private CpfIdentity(String cpf) {
        this.cpf = cpf;
    }

    @Override
    public Map<String, String> fields() {
        return Map.of("guestCpf", cpf);
    }

    @Override
    protected String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected List<String> partyLines(ReceiptData d) {
        return List.of("Hóspede: " + d.guestName() + " (CPF " + cpf + ")");
    }
}
