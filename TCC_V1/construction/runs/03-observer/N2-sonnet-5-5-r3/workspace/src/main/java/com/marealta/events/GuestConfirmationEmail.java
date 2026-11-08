package com.marealta.events;

import com.marealta.domain.Channel;
import com.marealta.domain.MessageOutbox;
import com.marealta.domain.Receipt;
import com.marealta.domain.Reservation;

/** O hóspede recebe o comprovante quando a reserva é confirmada. */
public final class GuestConfirmationEmail implements ReservationListener {
    private final MessageOutbox outbox;

    public GuestConfirmationEmail(MessageOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event.type() != ReservationEvent.Type.CONFIRMED) {
            return;
        }
        Reservation r = event.reservation();
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r, event.room()).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        outbox.send(Channel.EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
