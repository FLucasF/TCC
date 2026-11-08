package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.ReceiptText;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationListener;

/** Avisa o hóspede, com o comprovante, quando a reserva é confirmada. */
public class GuestConfirmationNotice implements ReservationListener {

    private final MessageLog log;

    public GuestConfirmationNotice(MessageLog log) {
        this.log = log;
    }

    @Override
    public void onConfirmed(Reservation reservation) {
        String body = "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + ReceiptText.of(Receipt.of(reservation)) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        log.send(Channel.EMAIL, reservation.guest().email(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
