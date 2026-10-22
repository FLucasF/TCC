package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Receipt;

/** Envia ao hóspede o e-mail de confirmação, com o comprovante. */
public class GuestConfirmationEmail implements ReservationListener {
    private final MessageLog log;

    public GuestConfirmationEmail(MessageLog log) {
        this.log = log;
    }

    @Override
    public void onConfirmed(ReservationConfirmed event) {
        Receipt receipt = Receipt.of(event.reservation(), event.room());
        String body = "Olá, " + event.reservation().guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        log.sendEmail(event.reservation().guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
