package pousada.service;

import pousada.domain.Receipt;
import pousada.domain.Reservation;

public class Notifications {
    private final Outbox outbox;

    public Notifications(Outbox outbox) {
        this.outbox = outbox;
    }

    public void reservationConfirmed(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + ReceiptText.render(Receipt.of(reservation)) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        outbox.send(Channel.EMAIL, reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
