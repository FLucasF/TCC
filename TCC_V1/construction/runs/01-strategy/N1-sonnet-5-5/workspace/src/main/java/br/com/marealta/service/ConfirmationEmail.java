package br.com.marealta.service;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;

final class ConfirmationEmail {

    private static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private ConfirmationEmail() {
    }

    static Message of(Reservation reservation) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(reservation).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, "EMAIL", reservation.guestEmail(), SUBJECT, body);
    }
}
