package pousada.document;

import java.util.List;
import java.util.Map;

/** Comprovante de reserva, para quem se identifica com CPF. */
final class CpfReceipt extends DocumentType {
    @Override public String code() { return "CPF"; }
    @Override public List<String> fields() { return List.of("guestCpf"); }
    @Override public String title() { return "Comprovante de reserva"; }
    @Override protected Language language() { return Language.PT_BR; }

    @Override
    protected List<String> identityLines(String guestName, Map<String, String> identity) {
        return List.of("Hóspede: " + guestName + " (CPF " + identity.get("guestCpf") + ")");
    }
}
