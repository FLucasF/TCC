package br.com.marealta.service;

import br.com.marealta.domain.Channel;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;

final class ConfirmationMessage {
    private ConfirmationMessage() {
    }

    static Message of(Reservation r) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + r.receipt().toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, Channel.EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
