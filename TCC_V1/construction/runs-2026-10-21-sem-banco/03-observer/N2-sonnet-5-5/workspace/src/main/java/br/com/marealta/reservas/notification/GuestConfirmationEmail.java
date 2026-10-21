package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Receipt;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationListener;

public class GuestConfirmationEmail implements ReservationListener {
    private final MessageOutbox outbox;

    public GuestConfirmationEmail(MessageOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void onConfirmed(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(reservation).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        outbox.email(reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
