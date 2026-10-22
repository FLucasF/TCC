package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.mail.Message;

final class ConfirmationEmail {
    private static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private ConfirmationEmail() {
    }

    static Message compose(Reservation reservation, Receipt receipt) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return Message.email(reservation.guestEmail(), SUBJECT, body);
    }
}
