package br.com.maralta.pousada;

import java.util.List;
import java.util.Map;

public record CpfIdentification(String cpf) implements Identification {
    static final Identifications.Kind KIND = new Identifications.Kind("CPF", List.of("guestCpf"),
            f -> new CpfIdentification(f.get("guestCpf")));

    @Override
    public Map<String, String> fields() {
        return Map.of("guestCpf", cpf);
    }

    @Override
    public Language language() {
        return Language.PT;
    }

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    public List<String> partyLines(String guestName) {
        return List.of("Hóspede: " + guestName + " (CPF " + cpf + ")");
    }
}
