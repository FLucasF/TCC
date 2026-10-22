package br.com.marealta.identity;

import java.util.List;
import java.util.Map;

final class CpfIdentity implements IdentityKind {
    public String code() { return "CPF"; }

    public List<String> fieldNames() { return List.of("guestCpf"); }

    public String documentTitle() { return "Comprovante de reserva"; }

    public Language language() { return Language.PT_BR; }

    public List<String> identityLines(String guestName, Map<String, String> values) {
        return List.of("Hóspede: " + guestName + " (CPF " + values.get("guestCpf") + ")");
    }
}
