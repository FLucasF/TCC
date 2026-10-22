package pousada.identity;

import java.util.List;
import java.util.Map;

public record CpfIdentity(String cpf) implements GuestIdentity {

    @Override
    public IdentityKind kind() {
        return IdentityKind.CPF;
    }

    @Override
    public Map<String, String> fields() {
        return Map.of("guestCpf", cpf);
    }

    @Override
    public DocumentLanguage language() {
        return DocumentLanguage.PORTUGUESE;
    }

    @Override
    public String documentTitle() {
        return "Comprovante de reserva";
    }

    @Override
    public List<String> partyLines(String guestName) {
        return List.of("Hóspede: " + guestName + " (CPF " + cpf + ")");
    }
}
