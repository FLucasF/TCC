package br.com.mare.pousada;

import java.util.List;
import java.util.Map;

public record CpfIdentity(String cpf) implements GuestIdentity {
    public static final IdentityKind KIND = new IdentityKind("CPF", List.of("guestCpf"),
            v -> new CpfIdentity(v.get("guestCpf")));

    @Override
    public IdentityKind kind() {
        return KIND;
    }

    @Override
    public Map<String, String> values() {
        return Map.of("guestCpf", cpf);
    }

    @Override
    public Document document(Reservation reservation) {
        return new ReceiptDocument(reservation, this);
    }
}
