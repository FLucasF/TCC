package br.com.marealta.pousada.messaging;

import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;

public final class ReservationEmails {
    private ReservationEmails() {
    }

    public static Message confirmation(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(reservation).toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return Message.email(reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
