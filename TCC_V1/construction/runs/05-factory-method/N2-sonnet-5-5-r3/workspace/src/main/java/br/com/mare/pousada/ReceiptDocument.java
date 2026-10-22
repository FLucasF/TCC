package br.com.mare.pousada;

import java.util.List;

public class ReceiptDocument extends Document {
    private final CpfIdentity guest;

    public ReceiptDocument(Reservation reservation, CpfIdentity guest) {
        super(reservation);
        this.guest = guest;
    }

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    protected DocumentStyle style() {
        return DocumentStyle.PORTUGUESE;
    }

    @Override
    protected List<String> guestLines() {
        return List.of("Hóspede: " + reservation.guestName() + " (CPF " + guest.cpf() + ")");
    }
}
