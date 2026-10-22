package pousada;

import java.util.List;

class CpfReceipt extends PortugueseDocument {
    @Override
    String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected List<String> identityLines(Reservation r) {
        return List.of("Hóspede: " + r.guestName() + " (CPF " + r.identification().values().get("guestCpf") + ")");
    }
}
