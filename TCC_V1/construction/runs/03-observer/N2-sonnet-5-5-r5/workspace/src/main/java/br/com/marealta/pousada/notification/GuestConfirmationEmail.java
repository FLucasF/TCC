package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Message;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.persistence.MessageRepository;

/** Manda ao hóspede o e-mail com o comprovante, quando a reserva é confirmada. */
public class GuestConfirmationEmail implements ReservationListener {
    private final MessageRepository outbox;

    public GuestConfirmationEmail(MessageRepository outbox) {
        this.outbox = outbox;
    }

    @Override
    public void onConfirmed(Reservation r) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + r.receipt().text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        outbox.send(Message.Channel.EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
