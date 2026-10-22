package br.com.marealta.domain;

import java.util.List;

/** Comprovante de reserva, para hóspede com CPF. */
final class ReceiptDocument extends PortugueseDocument {
    private final String cpf;

    ReceiptDocument(Reservation reservation, String cpf) {
        super(reservation);
        this.cpf = cpf;
    }

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected List<String> guestLines() {
        return List.of("Hóspede: " + reservation.guestName() + " (CPF " + cpf + ")");
    }
}
