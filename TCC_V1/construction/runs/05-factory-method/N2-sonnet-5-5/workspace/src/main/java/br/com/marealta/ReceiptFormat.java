package br.com.marealta;

import java.util.List;

public class ReceiptFormat extends PortugueseDocumentFormat {
    public String title() { return "Comprovante de reserva"; }

    protected List<String> partyLines(Reservation r) {
        return List.of("Hóspede: " + r.guestName() + " (CPF " + r.identity().get(CpfIdentity.CPF) + ")");
    }
}
