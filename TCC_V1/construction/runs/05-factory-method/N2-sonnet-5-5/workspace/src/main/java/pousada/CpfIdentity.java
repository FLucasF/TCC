package pousada;

import java.util.List;
import java.util.Map;

public record CpfIdentity(String cpf) implements GuestIdentity {
    public static final IdentityType TYPE = new IdentityType() {
        public List<String> keys() { return List.of("guestCpf"); }

        public GuestIdentity parse(RequestBody body) { return new CpfIdentity(body.text("guestCpf")); }
    };

    public Map<String, String> fields() { return Map.of("guestCpf", cpf); }

    public ReservationDocument issue(DocumentData data) { return new Comprovante(data, this); }

    static final class Comprovante extends ReservationDocument {
        private final CpfIdentity cpf;

        Comprovante(DocumentData data, CpfIdentity cpf) {
            super(data, cpf);
            this.cpf = cpf;
        }

        protected String title() { return "Comprovante de reserva"; }

        protected DocumentLabels labels() { return DocumentLabels.PORTUGUESE; }

        protected List<String> guestLines() {
            return List.of("Hóspede: " + data.guestName() + " (CPF " + cpf.cpf() + ")");
        }
    }
}
