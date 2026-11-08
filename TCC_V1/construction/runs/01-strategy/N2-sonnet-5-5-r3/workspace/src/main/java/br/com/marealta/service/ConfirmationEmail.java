package br.com.marealta.service;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;

final class ConfirmationEmail {
    private ConfirmationEmail() {}

    static Message of(Reservation r, Room room) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r, room).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, Message.EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
