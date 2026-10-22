package pousada.service;

import pousada.document.Document;
import pousada.domain.Channel;
import pousada.domain.Message;
import pousada.domain.Reservation;

/** O e-mail de confirmação: sempre em português; muda só o documento que vai dentro dele. */
final class ConfirmationEmail {
    private ConfirmationEmail() {
    }

    static Message of(Reservation r, Document document) {
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + document.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return new Message(0, Channel.EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
