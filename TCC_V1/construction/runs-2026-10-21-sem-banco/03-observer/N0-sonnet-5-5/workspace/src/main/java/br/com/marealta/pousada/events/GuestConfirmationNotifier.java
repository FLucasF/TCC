package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;

public class GuestConfirmationNotifier implements ReservationListener {
    private final Outbox outbox;

    public GuestConfirmationNotifier(Outbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void on(ReservationEvent event) {
        if (event instanceof ReservationEvent.Confirmed(Reservation r)) {
            String body = "Olá, " + r.guestName() + "!\n\n"
                    + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                    + Receipt.of(r).text() + "\n\n"
                    + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                    + "Até breve!";
            outbox.sendEmail(r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
        }
    }
}
