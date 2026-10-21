package pousada.identity;

import java.util.List;
import java.util.Map;

public record CpfIdentity(String cpf) implements GuestIdentity {
    static final String TITLE = "Comprovante de reserva";

    static final IdentityKind KIND = new IdentityKind(List.of("guestCpf"),
            values -> new CpfIdentity(values.get("guestCpf")));

    @Override
    public Map<String, String> fields() {
        return Map.of("guestCpf", cpf);
    }

    @Override
    public String documentTitle() {
        return TITLE;
    }

    @Override
    public String renderDocument(DocumentData data) {
        return DocumentLayout.render(DocumentLayout.PORTUGUESE, TITLE, data,
                List.of("Hóspede: " + data.guestName() + " (CPF " + cpf + ")"), List.of());
    }
}
