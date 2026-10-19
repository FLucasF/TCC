package pousada.domain.identification;

import pousada.domain.document.Document;
import pousada.domain.document.PortugueseLayout;
import pousada.domain.document.Voucher;

import java.util.List;
import java.util.Map;

/** Pessoa com CPF: recebe o comprovante de reserva. */
record CpfIdentification(String cpf) implements Identification {

    static final String TITLE = "Comprovante de reserva";

    @Override
    public Map<String, String> fields() {
        return Map.of("guestCpf", cpf);
    }

    @Override
    public Document issue(Voucher v) {
        String text = PortugueseLayout.render(TITLE, v,
                List.of("Hóspede: " + v.guestName() + " (CPF " + cpf + ")"), List.of());
        return new Document(TITLE, v, IdentificationKinds.withNulls(this), text);
    }

    static final class Kind implements IdentificationKind {
        @Override
        public List<String> fieldNames() {
            return List.of("guestCpf");
        }

        @Override
        public Identification create(Map<String, String> values) {
            return new CpfIdentification(values.get("guestCpf"));
        }
    }
}
