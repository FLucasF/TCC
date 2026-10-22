package pousada.identity;

import pousada.document.IssuedDocument;
import pousada.document.Stay;
import pousada.document.Wording;

import java.util.List;
import java.util.Map;

public record CpfIdentity(String cpf) implements Identity {
    private static final String TITLE = "Comprovante de reserva";

    public static final IdentityKind KIND = new IdentityKind() {
        @Override
        public String name() {
            return "CPF";
        }

        @Override
        public List<String> fieldNames() {
            return List.of("guestCpf");
        }

        @Override
        public Identity create(Map<String, String> fields) {
            return new CpfIdentity(Fields.required(fields, "guestCpf"));
        }
    };

    @Override
    public IdentityKind kind() {
        return KIND;
    }

    @Override
    public Map<String, String> fields() {
        return Map.of("guestCpf", cpf);
    }

    @Override
    public IssuedDocument issue(Stay stay) {
        String guest = "Hóspede: " + stay.guestName() + " (CPF " + cpf + ")";
        return new IssuedDocument(TITLE, Wording.PORTUGUESE.render(TITLE, stay, List.of(guest), List.of()));
    }
}
